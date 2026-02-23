package com.example.reverseshell;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Parcel;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Bundle;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import android.util.Log;

public class ReverseShellHandler {
    protected static String USER = Shell.exec("id -nu").trim();
    /*private static final BlockingQueue<Pair<String, String>> commands = new LinkedBlockingQueue<>();
    private static final KeyedBlockingQueue<String, String> results = new KeyedBlockingQueue<>();*/

    public static boolean handleInShell(Context context, Intent intent) {
        if (ReverseShellReceiver.InShellAction != intent.getAction()) {
            return false;
        }

        Bundle bundle = intent.getBundleExtra("data");
        if (bundle == null) {
            return false;
        }

        IBinder binder = bundle.getBinder("binder");
        byte[] msg = bundle.getByteArray("msg");

        Parcel data = null;
        Parcel reply = null;
        try {
            // 安全处理 msg 字节数组
            String user = (msg != null && msg.length > 0) ? new String(msg, "UTF-8") : USER;
            USER = user;

            // 从队列中获取命令
			Log.i("RShell", "等待可用命令");
            Pair<String, String> commandPair = ReverseShellServer.commands.take(); // 使用 poll 避免阻塞
            if (commandPair == null) {
                return false;
            }

            String id = commandPair.key;
            String command = commandPair.value;
			
			Log.i("RShell", id + " " + command);

            data = Parcel.obtain();
            reply = Parcel.obtain();

            Bundle commandBundle = new Bundle();
            commandBundle.putByteArray("msg", command.getBytes("UTF-8"));

            data.writeStrongBinder(new ReverseShellBinder());
            data.writeBundle(commandBundle);
            data.setDataPosition(0);

            binder.transact(1, data, reply, 2); // 使用默认标志

            String output = reply.readString();
            ReverseShellServer.results.put(id, output); // 确保线程安全
            return true;
        } catch (RemoteException | InterruptedException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            // 释放 Parcel 资源
            if (data != null) data.recycle();
            if (reply != null) reply.recycle();
        }
        return false;
    }

    @Deprecated
    public static boolean handleOutShell(Context context, Intent intent) {
        if (ReverseShellReceiver.OutShellAction != intent.getAction() || true) {
            return false;
        }

        Bundle bundle = intent.getBundleExtra("data");
        if (bundle == null) {
            return false;
        }

        IBinder binder = bundle.getBinder("binder");

        Parcel data = null;
        try {
            data = Parcel.obtain();
            data.writeStrongBinder(new ReverseShellBinder());
            data.writeString("shell");
            data.setDataPosition(0);

            binder.transact(1, data, null, IBinder.FLAG_ONEWAY);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            if (data != null) data.recycle();
        }
        return false;
    }

    @Deprecated
    public static boolean handleShell(Context context, Intent intent) {
        if (ReverseShellReceiver.ShellAction != intent.getAction() || true) {
            return false;
        }

        /*Bundle bundle = intent.getBundleExtra("data");
        if (bundle == null) {
            return false;
        }

        IBinder binder = bundle.getBinder("binder");

        Parcel data = null;
        Parcel reply = null;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();

            data.writeStrongBinder(new ReverseShellBinder());

            Pair<String, String> commandPair = commands.poll(); // 使用 poll 避免阻塞
            if (commandPair == null) {
                return false;
            }

            String command = commandPair.value;
            data.writeString(command);
            data.setDataPosition(0);

            binder.transact(1, data, reply, 0); // 使用默认标志

            String output = reply.readString();
            results.put(command, output); // 确保线程安全
            return true;
        } catch (RemoteException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (data != null) data.recycle();
            if (reply != null) reply.recycle();
        }*/
        return false;
    }
}

