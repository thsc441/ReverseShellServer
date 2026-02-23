package com.example.reverseshell;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

public class KeyedBlockingQueue<K, V> {
    private final ConcurrentHashMap<K, BlockingQueue<V>> queues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<K, Object> locks = new ConcurrentHashMap<>();

    public void put(K key, V value) throws InterruptedException {
        BlockingQueue<V> queue = queues.computeIfAbsent(key, new Function<K, BlockingQueue<V>>() {
                @Override
                public BlockingQueue<V> apply(K k) {
                    return new LinkedBlockingQueue<V>();
                }
            });
        queue.put(value);
        synchronized (getLock(key)) {
            getLock(key).notifyAll(); // 通知所有等待该 key 的消费者
        }
    }

    public V take(K key) throws InterruptedException {
        BlockingQueue<V> queue;
        while ((queue = queues.get(key)) == null) {
            Object lock = getLock(key);
            synchronized (lock) {
                lock.wait(); // 等待 key 被创建
            }
        }
        return queue.take(); // 队列存在后，继续阻塞等待数据
    }

    private Object getLock(K key) {
        return locks.computeIfAbsent(key, new Function<K, Object>() {
                @Override
                public Object apply(K k) {
                    return new Object();
                }
            });
    }
}

