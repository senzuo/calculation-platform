package com.chh.dc.calc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 有序的队列缓存，带有线程读写安全锁。是对LinkedList进行的包装
 * 
 * @ClassName: SynLinkedBuffer
 * @since 1.0
 * @version 1.0
 * @author Niow
 * @date: 2016-6-27
 */
public class SynLinkedBuffer<E> {

	private final ReentrantLock lock;

	private final LinkedList<E> bufferList = new LinkedList<E>();

	public SynLinkedBuffer() {
		lock = new ReentrantLock();
	}

	/**
	 * 获取缓存尺寸
	 * 
	 * @return
	 */
	public int size() {
		lock.lock();
		try {
			return bufferList.size();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 添加一个缓存元素
	 * 
	 * @param element
	 */
	public void add(E element) {
		lock.lock();
		try {
			bufferList.add(element);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 添加元素集合
	 * 
	 * @param alarms
	 */
	public void addAll(Collection<E> elements) {
		lock.lock();
		try {
			bufferList.addAll(elements);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 获取一个元素
	 * 
	 * @return
	 */
	public E take() {
		E alarm = null;
		lock.lock();
		try {
			if (bufferList.size() > 0) {
				alarm = bufferList.get(0);
				bufferList.remove(0);
			}
			return alarm;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * 一次获取多个元素，当缓存size小于number时，返回的元素个数等于size
	 * 
	 * @param number
	 * @return
	 */
	public List<E> take(int number) {
		List<E> tempList;
		if (number > bufferList.size()) {
			tempList = new ArrayList<E>(bufferList.size());
			lock.lock();
			try {
				while (bufferList.size() > 0) {
					tempList.add(bufferList.get(0));
					bufferList.remove(0);
				}
			} finally {
				lock.unlock();
			}
		} else {
			tempList = new ArrayList<E>(number);
			lock.lock();
			try {
				for (int i = 0; i < number; i++) {
					if (bufferList.size() > 0) {
						tempList.add(bufferList.get(0));
						bufferList.remove(0);
					}
				}
			} finally {
				lock.unlock();
			}
		}
		return tempList;
	}
}
