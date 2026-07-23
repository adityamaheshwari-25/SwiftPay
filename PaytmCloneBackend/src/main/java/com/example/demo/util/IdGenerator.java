package com.example.demo.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 *I used ThreadLocalRandom instead of Random because it performs better in multithreaded environments. 
 *It avoids contention by maintaining a separate random generator per thread. 
 *
 *These methods generates a merchant ID by combining the current timestamp and a random number. 
 *The timestamp ensures time-based uniqueness and chronological ordering. The random number reduces the risk 
 *of collisions within the same millisecond. Both values are converted to Base-36 to make the ID shorter 
 *and alphanumeric. I used ThreadLocalRandom because it performs better in concurrent environments compared 
 *to Random, avoiding contention issues.
 *
 *Contention happens when multiple threads try to access the same shared resource at the same time.
 *
 *Why ThreadLocalRandom Reduces Contention
	ThreadLocalRandom:
		Each thread gets its own random generator.
		No shared state.
		No waiting.
		No locking.
		Better performance in multi-threaded apps (like backend servers).
 */
public class IdGenerator {

	/**
	 *This method generates a merchant ID by combining the current timestamp and a random number, 
	 *converting both to Base-36 to make the ID shorter and alphanumeric, and prefixing it with ‘MID’ 
	 */
    // Merchant ID generator
    public static String generateMerchantId() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);

        /**
         *The 36 represents the radix for number conversion. It converts the number into Base-36 format, which uses digits 0–9 and letters A–Z.
         *	Makes the ID shorter.
			Makes it alphanumeric.
			More compact than decimal representation. 
         */
        String base = Long.toString(timestamp, 36).toUpperCase();
        String rand = Integer.toString(random, 36).toUpperCase();
        
        System.out.println("MID" + base + rand);

        return "MID" + base + rand;
    }

    // Transaction ID generator
    public static String generateTxId() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);

        String base = Long.toString(timestamp, 36).toUpperCase();
        String rand = Integer.toString(random, 36).toUpperCase();

        return "TXN" + base + rand;
    }

    // Reference ID generator (same for SEND + RECEIVE pair)
    public static String generateReferenceId() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);

        String base = Long.toString(timestamp, 36).toUpperCase();
        String rand = Integer.toString(random, 36).toUpperCase();

        return "REF" + base + rand;
    }
    
    public static String generateSplitId() {
        long timestamp = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(10000, 99999);

        String base = Long.toString(timestamp, 36).toUpperCase();
        String rand = Integer.toString(random, 36).toUpperCase();

        return "SPL" + base + rand;
    }
    
    // generating random account number.
    public static String generateAccountNumber() {
        long number = ThreadLocalRandom.current()
                .nextLong(100000000000L, 999999999999L);
        return "AC" + number;
    }
}
