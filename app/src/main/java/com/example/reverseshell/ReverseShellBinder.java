package com.example.reverseshell;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.io.FileDescriptor;

public class ReverseShellBinder implements IBinder {

    @Override
    public void dump(FileDescriptor fd, String[] args) throws RemoteException {
    }

    @Override
    public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {
    }

    @Override
    public String getInterfaceDescriptor() throws RemoteException {
        return null;
    }

    @Override
    public boolean isBinderAlive() {
        return false;
    }

    @Override
    public void linkToDeath(IBinder.DeathRecipient recipient, int flags) throws RemoteException {
    }

    @Override
    public boolean pingBinder() {
        return false;
    }

    @Override
    public IInterface queryLocalInterface(String descriptor) {
        return null;
    }

    @Override
    public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return false;
    }

    @Override
    public boolean unlinkToDeath(IBinder.DeathRecipient recipient, int flags) {
        return false;
    }
}
