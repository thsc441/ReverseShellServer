package com.example.reverseshell;

import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReverseShellLoader {
	protected static String USER = Shell.exec("id -nu").trim();
    private static Handler handler;
	
	private static final Object lock = new Object(); // 用于同步访问
	private static CountDownLatch latch = new CountDownLatch(1); // 用于等待响应
	private static volatile boolean isProcessing = false; // 标记是否正在处理
	private static Thread requestForBinder;
    private static final Binder receiverBinder = new Binder() {
        @Override
		protected boolean onTransact(int code, Parcel data, final Parcel reply, int flags) throws RemoteException {
			if (code == 1) {
				data.setDataPosition(0);
				IBinder binder = data.readStrongBinder();
				Bundle commandBundle  = data.readBundle();
				if (commandBundle == null) {
					return false;
				}
				String command = new String(commandBundle.getByteArray("msg"));
				
				if (command != null) { //傻逼系统 binder 为 null command 不为 null !!!
					try {
						// 执行命令并写入结果
						String result = Shell.exec(command);
						//System.out.println(command + " - " + result);
						reply.writeString(result);
					} catch (Exception e) {
						reply.writeString("Error: " + e.getMessage());
					} finally {
						reply.setDataPosition(0); // 确保 Parcel 正确写入
						latch.countDown(); // 响应完成后释放等待的线程
					}
				} else {
					System.err.println("Server is not running");
					System.err.flush();
					System.exit(1);
				}
				return true;
			}
			return super.onTransact(code, data, reply, flags);
		}
    };
    
    private static void requestForBinder() {
		requestForBinder = new Thread(new Runnable() {
				@Override
				public void run() {
					while (!Thread.currentThread().isInterrupted()) {
						USER = Shell.exec("id -nu").trim();
						//System.out.println(USER);
						Parcel data = Parcel.obtain();
						data.writeString(USER);
						data.setDataPosition(0);
						send(data);
					}
				}
			});
		requestForBinder.start();
    }

    private static void onBinderReceived(IBinder binder, String sourceDir) {
        
    }

    public static void main(String[] args) {
        if (Looper.getMainLooper() == null) {
            Looper.prepareMainLooper();
        }

        handler = new Handler(Looper.getMainLooper());

        try {
            requestForBinder();
			new ReLoadThread().start();
			Thread.sleep(Long.MAX_VALUE);
        } catch (Throwable tr) {
            tr.printStackTrace(System.err);
            System.err.flush();
            System.exit(1);
        }

        Looper.loop();
        System.exit(0);
    }
	
	private static void send(Parcel msgData) {
		synchronized (lock) {
			if (isProcessing) {
				try {
					lock.wait(); // 如果正在处理，等待
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			isProcessing = true; // 标记为处理中
		}

		try {
			// 原有发送逻辑
			Bundle bunder = new Bundle();
			bunder.putBinder("binder", receiverBinder);
			String msg = msgData.readString();
			bunder.putByteArray("msg", msg.getBytes());

			Intent intent = new Intent(ReverseShellReceiver.InShellAction)
				.setPackage("com.example.reverseshell")
				.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
				.putExtra("data", bunder);

			IBinder amBinder = ServiceManager.getService("activity");
			IActivityManager am;
			if (Build.VERSION.SDK_INT >= 26) {
				am = IActivityManager.Stub.asInterface(amBinder);
			} else {
				am = ActivityManagerNative.asInterface(amBinder);
			}

			try {
				am.broadcastIntent(null, intent, null, null, 0, null, null,
								   null, -1, null, true, false, 0);
			} catch (Throwable e) {
				e.printStackTrace();
			}

			// 等待响应完成
			try {
				if (!latch.await(5, TimeUnit.SECONDS)) {
					
				}
				// 阻塞 5s 直到响应处理完成
			} catch (InterruptedException e) {
				//e.printStackTrace();
			}
		} finally {
			synchronized (lock) {
				isProcessing = false; // 标记处理完成
				lock.notify(); // 通知下一个等待的线程
			}
			latch = new CountDownLatch(1); // 重置计数器
		}
	}
	

    private static void abort(String message) {
        System.err.println(message);
        System.err.flush();
        System.exit(1);
    }
	
	private static class ReLoadThread extends Thread {
		@Override
		public void run() {
			//System.out.println("启动 ReLoadThread");
			try {
				WatchService watchService = FileSystems.getDefault().newWatchService();
				Path FILE_DIR = Paths.get("/data/local/tmp");
				FILE_DIR.register(watchService,StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);
				
				while (true) {
					WatchKey key = watchService.take();
					for (WatchEvent<?> event : key.pollEvents()) {
						WatchEvent.Kind<?> kind = event.kind();
						if (kind == StandardWatchEventKinds.OVERFLOW) {
							continue;
						}
						// 获取文件名
						Path fileName = (Path) event.context();
						final Path fullPath = FILE_DIR.resolve(fileName);
						if (Files.isRegularFile(fullPath) && fileName.toString().equals("RELOAD")) {
							ReLoad();
						}
					}
					// 重置 WatchKey
					boolean valid = key.reset();
					if (!valid) {
						break;
					}
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		public static void ReLoad() {
			if (requestForBinder != null) {
				requestForBinder.interrupt();
			}
			if (latch != null) {
				latch.countDown();
			}
			requestForBinder = null;
			System.gc();
			requestForBinder();
		}
	}
}
