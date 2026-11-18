/*
	This file is part of FreeJ2ME.
	FreeJ2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
*/
package javax.microedition.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.wireless.messaging.impl.MessageConnectionImpl;
import org.recompile.mobile.Mobile;

public class Connector {
    public static final int READ = 1;
    public static final int READ_WRITE = 3;
    public static final int WRITE = 2;

    public static InputStream openInputStream(String name) throws IOException {
        if (name != null && name.startsWith("resource:")) {
            return Mobile.getPlatform().loader.getMIDletResourceAsSiemensStream(
                name.substring(9).replaceAll("\\\\", "/"));
        }
        Connection connection = open(name, READ);
        if (!(connection instanceof InputConnection)) {
            connection.close();
            throw new IOException("Connection does not support input streams");
        }
        final InputStream stream = ((InputConnection) connection).openInputStream();
        return new FilterInputStream(stream) {
            public void close() throws IOException {
                try { super.close(); } finally { connection.close(); }
            }
        };
    }

    public static DataInputStream openDataInputStream(String name) throws IOException {
        return new DataInputStream(openInputStream(name));
    }

    public static Connection open(String name) throws IOException {
        if (name != null && name.startsWith("sms://")) return new MessageConnectionImpl(name);
        return GcfNetwork.open(name, READ_WRITE, false);
    }

    public static Connection open(String name, int mode) throws IOException {
        if (name != null && name.startsWith("sms://")) return new MessageConnectionImpl(name);
        return GcfNetwork.open(name, mode, false);
    }

    public static Connection open(String name, int mode, boolean timeouts) throws IOException {
        if (name != null && name.startsWith("sms://")) return new MessageConnectionImpl(name);
        return GcfNetwork.open(name, mode, timeouts);
    }

    public static DataOutputStream openDataOutputStream(String name) throws IOException {
        return new DataOutputStream(openOutputStream(name));
    }

    public static OutputStream openOutputStream(String name) throws IOException {
        Connection connection = open(name, WRITE);
        if (!(connection instanceof OutputConnection)) {
            connection.close();
            throw new IOException("Connection does not support output streams");
        }
        final OutputStream stream = ((OutputConnection) connection).openOutputStream();
        return new FilterOutputStream(stream) {
            public void close() throws IOException {
                try { super.close(); } finally { connection.close(); }
            }
        };
    }
}
