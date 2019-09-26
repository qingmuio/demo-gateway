package io.qingmu.demogateway.gray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;


public class WeigherTest {
    public static void main(String[] args) {

        Map<String, Integer> serverMap = new HashMap<String, Integer>() {{
            put("192.168.1.100", 95);
            put("192.168.1.101", 5);
//            put("192.168.1.102", 4);
//            put("192.168.1.103", 1);
//            put("192.168.1.104", 1);
//            put("192.168.1.105", 3);
//            put("192.168.1.106", 1);
//            put("192.168.1.107", 2);
//            put("192.168.1.108", 1);
//            put("192.168.1.109", 1);
//            put("192.168.1.110", 1);
        }};
        Map<String, AtomicLong> map = new HashMap<>();
        for (int i = 0; i < 1_000; i++) {
            final String ip = randomWeight(serverMap);
            final boolean b = map.containsKey(ip);
            if (b) {
                map.get(ip).incrementAndGet();
            } else {
                map.put(ip, new AtomicLong(1));
            }
        }

        System.out.println(map);


    }

    public static String randomWeight(Map<String, Integer> serverMap) {
        int length = serverMap.size(); // 总个数
        final ArrayList<String> servers = new ArrayList<>(serverMap.keySet());
        int totalWeight = 0; // 总权重
        boolean sameWeight = true; // 权重是否都一样
        for (int i = 0; i < length; i++) {
            int weight = serverMap.get(servers.get(i));
            totalWeight += weight; // 累计总权重
            if (sameWeight && i > 0
                    && weight != serverMap.get(servers.get(i - 1))) {
                sameWeight = false; // 计算所有权重是否一样
            }
        }
        if (totalWeight > 0 && !sameWeight) {
            // 如果权重不相同且权重大于0则按总权重数随机
            int offset = ThreadLocalRandom.current().nextInt(totalWeight);
            // 并确定随机值落在哪个片断上
            for (int i = 0; i < length; i++) {
                offset -= serverMap.get(servers.get(i));
                if (offset < 0) {
                    return servers.get(i);
                }
            }
        }
        // 如果权重相同或权重为0则均等随机
        return servers.get(ThreadLocalRandom.current().nextInt(length));
    }

}
