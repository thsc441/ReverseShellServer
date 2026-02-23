package com.example.reverseshell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.UUID;
import android.util.Log;

public class ReverseShellServer {
    public static BlockingQueue<Pair<String, String>> commands = new LinkedBlockingQueue<>(255);
    public static KeyedBlockingQueue<String, String> results = new KeyedBlockingQueue<>();
    
    //public static KeyedBlockingQueue<String, String> commands = new KeyedBlockingQueue<>();
    private static ServerSocket server;

    public static void start() {
        if (server != null) return;

        try {
            server = new ServerSocket(4446);
			System.out.println("TerminalServer 启动");
            new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                final Socket client = server.accept();
                                new Thread(new Runnable(){
                                        @Override
                                        public void run() {
                                            handleClient(client);
                                        }
                                    }).start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket client) {
		// 使用try-with-resources自动关闭流和Socket
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
			OutputStream out = client.getOutputStream();

			// 读取客户端发送的ID
			String command = in.readLine();
			if (command == null) {
				// 处理客户端异常断开的情况
				out.write("null command".getBytes());
				out.flush();
				return;
			}
			String trimmedCommand = command.trim();

			String output = "";
			if (trimmedCommand.equals("USER")) {
				output = ReverseShellHandler.USER;
			} else {
				String id = UUID.randomUUID().toString();
				Log.i("RShell", "收到命令 - " + trimmedCommand + " 分配 id - " + id);
				commands.put(new Pair<String, String>(id, trimmedCommand));
				output = results.take(id);
			}

			Log.i("RShell", trimmedCommand + " - " + output);

			if (output != null) {
				// 显式指定字符编码
				out.write(output.getBytes(StandardCharsets.UTF_8));
				out.flush();
			} else {
				// 发送超时响应
				out.write("ReverseShellService not Running".getBytes(StandardCharsets.UTF_8));
				out.flush();
			}
		} catch (IOException e) {
			// 记录IO异常
			System.err.println("IO error handling client: " + e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			// 确保Socket关闭
			try {
				client.close();
			} catch (IOException e) {
				System.err.println("Error closing socket: " + e.getMessage());
			}
		}
	}
}
