package com.hp.octane.plugins.jenkins.tests.impl;

import hudson.FilePath;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ObjectStreamIterator<E> implements Iterator<E> {
	private static Logger logger = LogManager.getLogger(ObjectStreamIterator.class);

	private MyObjectInputStream ois;
	private FilePath filePath;
	private E next;

	public ObjectStreamIterator(FilePath filePath, boolean deleteOnClose) throws IOException, InterruptedException {
		this.filePath = filePath;
		ois = new MyObjectInputStream(new BufferedInputStream(filePath.read()), deleteOnClose);
	}

	@Override
	public boolean hasNext() {
		if (next != null) {
			return true;
		}
		try {
			next = (E) ois.readObject();
			return true;
		} catch (Exception e) {
			logger.error("Failed to read the next item", e);
			ois.close();
			return false;
		}
	}

	@Override
	public E next() {
		if (hasNext()) {
			E value = next;
			next = null;
			return value;
		} else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private class MyObjectInputStream extends ObjectInputStream {

		private boolean deleteOnClose;

		public MyObjectInputStream(InputStream in, boolean deleteOnClose) throws IOException {
			super(in);
			this.deleteOnClose = deleteOnClose;
		}

		@Override
		public void close() {
			try {
				super.close();
			} catch (IOException ioe) {
				logger.error("Failed to close the stream", ioe); // NON-NLS
			}
			if (deleteOnClose) {
				try {
					filePath.delete();
				} catch (Exception e) {
					logger.error("Failed to perform clean up", e); // NON-NLS
				}
			}
		}
	}
}
