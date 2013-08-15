import java.util.*;
import java.io.*;
import java.text.*;

/** 
 * Represents a Cache of 2^c bytes data, 2^b byte blocks, 2^s associativity, and 
 * p blocks prefetched upon a miss.
 *
 * @author Hannah Lau
 * @version 6.0
 */
public class Cache {
	private int c, b, s, p;
	private Block[][] cache;
	private int numAccessesToCache = 0, numReads = 0, numReadMisses = 0, numWrites = 0, 
		numWriteMisses = 0, numWriteBacks = 0, totBlocksPrefetced = 0;
	
	private int bitsBlockOffset, bitsCacheIndex, bitsCacheTag, instrNum = 1;
	private DecimalFormat fmt3, fmt4, fmt8;
	private Timer timer;
	private boolean debug;

	/**
	 * Runs the cache simulator. 
	 *
	 * @param String[] args The specifications for the cache, following the format
	 * "filename.txt -c # -b # -s # -p #".
	 */
	public static void main(String[] args) throws IOException {
		//Runs the cache simulator
		if(args.length == 9) {
			BufferedReader br = new BufferedReader(new FileReader(args[0]));
			
			int c = Integer.parseInt(args[2]);
			int b = Integer.parseInt(args[4]); 
			int s = Integer.parseInt(args[6]); 
			int p = Integer.parseInt(args[8]);
			
			String input = "";
			Scanner scan = new Scanner(System.in);
			do {
				System.out.println("Debug? [y/n]");
				input = scan.nextLine();
			} while(!(input.equals("y") || input.equals("n")));

			scan.close();
			
			Cache cache = new Cache(c, b, s, p, br, input.equals("y"));

			//Reads in lines of the text file until end of file
			if(!input.equals("y")) {
				String line = br.readLine();
				while(line != null) {
					cache.run(line);
					line = br.readLine();
				}

				System.out.println("\n\n" + cache.stats());
				DecimalFormat fmt = new DecimalFormat("0.000");
				System.out.println("\nEMAT: " + fmt.format(cache.emat()) + "\n");
				System.exit(0);
			}
		}
		//Runs the cache experiment
		else if(args.length == 2) {
			
			int totalSpace = Integer.parseInt(args[1])*1024*8;
			int bestC = 0, bestB = 0, bestS = 0, bestP = 0;
			double bestEMAT = 99999;

			//c, b, s, and p loop within lower and upper bounds to find best cache stats
			for(int c = 1; c <= (int)(Math.log(totalSpace)/Math.log(2)); c++)
				for(int b = 0; b <= c; b++)
					for(int s = 0; s <= c-b && totalBitsInCache(c, b, s) <= totalSpace; s++)
						for(int p = 0; p < (int)Math.pow(2, c-b-s) && p < 50; p++) {
							System.out.println("Run: -c " + c + " -b " + b + " -s " + s + " -p " + p);

							BufferedReader br = new BufferedReader(new FileReader(args[0]));
							Cache cache = new Cache(c, b, s, p, br, false);

							String line = br.readLine();

							while(line != null) {
								cache.run(line);
								line = br.readLine();
							}

							if(cache.emat() < bestEMAT) {
								bestC = c;
								bestB = b;
								bestS = s;
								bestP = p;
								bestEMAT = cache.emat();
								System.out.println("Found better EMAT\nCache stats: -c " + bestC + " -b " 
									+ bestB + " -s " + bestS + " -p " + bestP + "\nEMAT: " + bestEMAT);
							}
						}

			System.out.println("\nResults for file " + args[0] + "\nCache stats: -c " + bestC + " -b " 
				+ bestB + " -s " + bestS + " -p " + bestP + "\nEMAT: " + bestEMAT);
		}	 
		else
			System.out.println("Improper input, please enter in follow format: filename.txt -c # -b # -s # -p #" + 
				"\nEx. input.txt -c 10 -b 4 -s 2 -p 4 or input.txt 48");
	}

	/**
	 * Creates and simulates a cache according to the input textfile.
	 *
	 * @param int c Cache size.
	 * @param int b Block size.
	 * @param int s Set associativity.
	 * @param int p Prefetch blocks.
	 * @param BufferedReader bf The reader for the input textfile. 
	 * @param boolean debug Indicates whether to simulate in debug mode.
	 */
	public Cache(int c, int b, int s, int p, BufferedReader br, boolean debug) {
		this.c = c;
		this.b = b;
		this.s = s;
		this.p = p;
		this.debug = debug;

		//Creates cache of 2^c/(2^b*2^s) rows and 2^s columns
		cache = new Block[pow(2, c)/(pow(2, b)*pow(2, s))][pow(2, s)];

		for(int row = 0; row < cache.length; row++) 
			for(int col = 0; col < cache[row].length; col++) 
				cache[row][col] = new Block();

		//Calculates number of bits in cache instruction interpretation (b, n, and t) 
		bitsBlockOffset = b;
		bitsCacheIndex = c-b-s;
		bitsCacheTag = 32-c+s;

		//Formatting output
		fmt3 = new DecimalFormat("000");
		fmt4 = new DecimalFormat("0.000");
		fmt8 = new DecimalFormat("00000000");

		//Timer executes lines of input textfile at a rate according to debug 
		if(debug && br != null) {
			timer = new Timer();
			timer.schedule(new Task(this, br), 500, (debug ? 1000 : 1));
		}
	}

	/**
	 * Task that the timer executes. If in debug mode, the cache will execute instructions
	 * at a rate of 1 instruction / second. Otherwise, the cache will execute instructions 
	 * with 0 delay.
	 *
	 * @author Hannah Lau
	 * @version 1.0
	 */
	private class Task extends TimerTask {
		BufferedReader br;
		Cache cache;
		String line;

		/**
		 * Creates the task for the timer.
		 *
		 * @param Cache cache A reference to the created cache.
		 * @param BufferedReader bf A reference to the cache's reader for input textfile.
		 */
		public Task(Cache cache, BufferedReader br) {
			this.cache = cache;
			this.br = br;
		}

		/**
		 * The action fired by the timer. Calls for the cache to read one line in the
		 * input textfile.
		 */
		public void run() {
			try {
				line = br.readLine();
				if(debug && line != null) {
					System.out.println("\n\nInstruction " + (instrNum++) + ": " + line
						+ " (" + binary(line.substring(2)) + ")");
					System.out.println(cache.stats());
				}

			}
			catch(Exception e) {
				e.printStackTrace();
			}

			//Call for the cache to execute a line of the input textfile
			if(line != null)
				cache.run(line);
			else {
				System.out.println("\n\n" + cache.stats());
				System.out.println("\nEMAT: " + fmt4.format(cache.emat()) + "\n");
				System.exit(0);
			}

			//Prints the contents of the cache every n instructions
			int n = 1;
			if(debug && instrNum%n == 0)
				System.out.println("\n" + cache + "\n");
		}

		/**
		 * Converts a string representation of a hex number into a binary representation
		 * of the number.
		 *
		 * @param String hex The string representation of a hex number.
		 *
		 * @return String The string of the hex in binary.
		 */
		private String binary(String hex) {
			String binary = "";

			for(int i = hex.length(); 0 < i; i--) {
				if(hex.substring(i-1, i).equals("0"))
					binary = " 0000" + binary;
				else if(hex.substring(i-1, i).equals("1"))
					binary = " 0001" + binary;
				else if(hex.substring(i-1, i).equals("2"))
					binary = " 0010" + binary;
				else if(hex.substring(i-1, i).equals("3"))
					binary = " 0011" + binary;
				else if(hex.substring(i-1, i).equals("4"))
					binary = " 0100" + binary;
				else if(hex.substring(i-1, i).equals("5"))
					binary = " 0101" + binary;
				else if(hex.substring(i-1, i).equals("6"))
					binary = " 0110" + binary;
				else if(hex.substring(i-1, i).equals("7"))
					binary = " 0111" + binary;
				else if(hex.substring(i-1, i).equals("8"))
					binary = " 1000" + binary;
				else if(hex.substring(i-1, i).equals("9"))
					binary = " 1001" + binary;
				else if(hex.substring(i-1, i).equals("a"))
					binary = " 1010" + binary;
				else if(hex.substring(i-1, i).equals("b"))
					binary = " 1011" + binary;
				else if(hex.substring(i-1, i).equals("c"))
					binary = " 1100" + binary;
				else if(hex.substring(i-1, i).equals("d"))
					binary = " 1101" + binary;
				else if(hex.substring(i-1, i).equals("e"))
					binary = " 1110" + binary;
				else if(hex.substring(i-1, i).equals("f"))
					binary = " 1111" + binary;
			}

			return binary.substring(1);
		}
	}

	/**
	 * Represents a block in the cache. 
	 * 
	 * @author Hannah Lau
	 * @version 1.0
	 */
	private class Block {
		int tag, used;
		boolean dirty, valid;

		/**
		 * Default constructor for the block.
		 */
		public Block() {
			this(0, false, 0, false);
		}

		/**
		 * Creates a block according to the input specificiations.
		 *
		 * @param int tag The block's tag.
		 * @param boolean valid Indicates if the block is in use.
		 * @param int used Counts how long the block has been unused.
		 * @param boolean dirty Indicates if block has been written to.
		 */
		public Block(int tag, boolean valid, int used, boolean dirty) {
			this.tag = tag;
			valid = false;
			this.used = used;
			this.dirty = false;
		}

		/**
		 * Block toString
		 * 
		 * @return String A character representation of the contents of a block
		 */
		public String toString() {
			return "[t]" + fmt8.format(tag) + " [v]" + (valid?1:0) + " [u]" + fmt3.format(used) + " [d]" + (dirty?1:0);
		}
	}

	/** 
	 * Formats an instruction to be run in the cache. 
	 *
	 * @param String line The line from the textfile.
	 */
	public void run(String line) {
		boolean read = line.substring(0,1).equals("r");
		run(read, hexToDec(line.substring(2)));
	}
	
	/**
	 * Runs an instruction from the input textfile.
	 *
	 * @param boolean read The r/w part of the instruction. 
	 * @param int addr The number part of the instruction.
	 */
	public void run(boolean read, int addr) {
		Block[] blocks = cache[getIndex(addr)%cache.length];
		int tag = getTag(addr);
		boolean found = false;

		if(debug)
			System.out.println((read ? "Reading" : "Writing"));

		numAccessesToCache++;
		
		if(read)
			numReads++;
		else
			numWrites++;

		for(int i = 0; i < blocks.length; i++) {
			//Increments each block's used field in this line of the cache for LRU
			if(blocks[i].valid)
				blocks[i].used++;

			//Checks for hit in the cache
			if(!found && blocks[i].valid && blocks[i].tag == tag) { 
				if(debug)
					System.out.println("Hit");

				found = true;
				blocks[i].used = 0;

				//Marks dirty bit for a write
				if(!read) 
					blocks[i].dirty = true;
			}
		}

		//Handle a cache miss
		if(!found)
			miss(addr, blocks, read, tag);
	}

	/**
	 * Handles a miss in the cache.
	 * 
	 * @param int addr The number part of the instruction.
	 * @param Block[] blocks The line of interest in the cache.
	 * @param int tag The cache tag of addr.
	 */
	private void miss(int addr, Block[] blocks, boolean read, int tag) {
		if(debug)
			System.out.println("Miss");

		if(read)
			numReadMisses++;
		else
			numWriteMisses++;

		boolean found = false;
		int max = -1, lru = -1;
		for(int i = 0; i < blocks.length && !found; i++) {
			//Searching for an empty block in the line
			if(!blocks[i].valid) {
				if(debug)
					System.out.println("Found empty spot");
				
				found = true;

				blocks[i].valid = true;
				blocks[i].tag = tag;
				blocks[i].used = 0;
				blocks[i].dirty = false;
			} 
			//Searching for LRU block
			else if(max < blocks[i].used) {
				lru = i;
				max = blocks[i].used;
			}
		}

		//If no empty block was found, write back then evict LRU block
		if(!found) {
			if(debug) 
				System.out.println("No empty spots, evicting block.");

			if(blocks[lru].dirty) 
				numWriteBacks++;	

			blocks[lru].valid = true;
			blocks[lru].tag = tag;
			blocks[lru].used = 0;
			blocks[lru].dirty = false;
		}		

		//Prefetch upon read misses
		if(read)
			prefetch(addr);
	}

	/** 
	 * Handles prefetching 2^p blocks.
	 *
	 * @param int addr The number part of the instruction. 
	 */
	private void prefetch(int addr) {
		Block[] blocks;
		int tag;
		boolean found;

		for(int i = 1; i <= p; i++) {
			//Increment the index part of the address to fetch next block
			addr += 1<<bitsBlockOffset;
			blocks = cache[getIndex(addr)%cache.length]; 
			tag = getTag(addr);
			found = false;

			//Check if the block is already in the cache before prefetching
			for(int j = 0; j < blocks.length; j++)
				if(blocks[j].valid && blocks[j].tag == tag) 
					found = true;
				
			//Find a spot to prefetch the block into 
			if(!found) {
				if(debug)
					System.out.println("Prefetching block: " + i);
				
				totBlocksPrefetced++;

				int max = -1, lru = -1;
				for(int j = 0; j < blocks.length && !found; j++) {
					//Searching for empty spot in the current line of the cache
					if(!blocks[j].valid) { 
						if(debug)
							System.out.println("Found empty spot while prefetching");
						
						found = true;

						blocks[j].valid = true;
						blocks[j].tag = tag;
						blocks[j].used = 0;
						blocks[j].dirty = false;
					} 
					//Searching for LRU block
					else if(max < blocks[j].used) {
						lru = j;
						max = blocks[j].used;
					}
				}

				//If no empty block was found, write back then evict LRU block
				if(!found) {
					if(debug)
						System.out.println("No empty spots, evicting block while prefetching");

					if(blocks[lru].dirty) 
						numWriteBacks++;

					blocks[lru].valid = true;
					blocks[lru].tag = tag;
					blocks[lru].used = 0;
					blocks[lru].dirty = false;
				}
			}
		}
	}

	/** 
	 * Converts a String representation of a hex value to decimal.
	 *
	 * @param String hex A String hex value.
	 * 
	 * @return int The decimal value of the hex number.
	 */
	private int hexToDec(String hex) {
		int dec = 0, pow = hex.length()-1;

		for(char d : hex.toCharArray())
			dec += ((97 <= d) ? d-87 : d-48)*pow(16, pow--); //like a boss

		return dec;
	}

	/**
	 * Extracts the cache tag from the address
	 *
	 * @param int memAddr The address.
	 *
	 * @param int The cache tag.
	 */
	private int getTag(int memAddr) {
		return memAddr/pow(2, (bitsCacheIndex + bitsBlockOffset));
	}

	/**
	 * Extracts the cache index from the address
	 *
	 * @param int memAddr The address.
	 *
	 * @param int The cache index.
	 */
	private int getIndex(int memAddr) {
		return (memAddr/pow(2, bitsBlockOffset))%pow(2, bitsCacheIndex);
	}
	
	/**
	 * Raises the base to a power.
	 *
	 * @param int The base.
	 * @param int The power.
	 *
	 * @return int base^power
	 */
	private int pow(int base, int power) {
		int total = 1;

		for(int i = 0; i < power && power != 0; i++)
			total = total*base;

		return total;
	}

	/**
	 * Returns a String representation of the cache statistics.
	 * 
	 * @return String A representation of the cache statistics.
	 */
	public String stats() {
		String stats = "Cache Dimensions\n-----------------\nc: " + c + " b: " + b + " s: " 
			+ s + " p: " + p + "\n";
	 	stats += "\nAccess to cache: " + numAccessesToCache;
		stats += "\nReads: " + numReads;
		stats += "\nRead misses: " + numReadMisses;
		stats += "\nWrites: " + numWrites;
		stats += "\nWrite misses: " + numWriteMisses;
		stats += "\nWrite backs in bytes: " + numWriteBacks*pow(2, b);

		stats += "\nTotal bytes transferred: " + ((numWriteBacks + numReadMisses 
			+ totBlocksPrefetced)*pow(2, b));
		stats += "\nTotal blocks prefetced: " + totBlocksPrefetced;
		stats += "\nTotal misses: " + (numReadMisses + numWriteMisses);
		stats += "\nMiss rate: " + fmt4.format(((double)numReadMisses)/(numReads));
		stats += "\nTotal number of bits of cache storage: " + totalBitsInCache();

		return stats;
	}

	/**
	 * Calculates the total number of bits in the cache, including data storage, tag
	 * storage, valid, and dirty bits.
	 * 
	 * @param int c Cache size.
	 * @param int b Block size.
	 * @param int s Set associativity. 
	 */
	public int totalBitsInCache() {
		//(total bytes data)*(8 bits/byte) + (bits cache tag + 1 valid bit + 1 dirty bit) 
		// * number of lines * number of columns
		return (pow(2,b)*8+(bitsCacheTag+2))*cache.length*pow(2,s);
	}

	/**
	 * Calculates the total number of bits in the cache, including data storage, tag
	 * storage, valid, and dirty bits.
	 * 
	 * @param int c Cache size.
	 * @param int b Block size.
	 * @param int s Set associativity. 
	 */
	public static int totalBitsInCache(int c, int b, int s) {
		return (int)(Math.pow(2,c)*(8+(32-c+s+2)/Math.pow(2,b))); 
	}

	/**
	 * Calculates the cache EMAT according to EMAT = Tc + m*Tm where Tc is the cache access 
	 * time, m is the miss rate, and Tm is the memory access time.
	 *
	 * @return double The cache EMAT.
	 */
	public double emat() {
		double missPenalty = 50, hitTime = 2+0.2*s;
	
		return hitTime + missPenalty*numReadMisses/numReads;
	}

	/**
	 * Returns a String representation of the contents of the entire cache.
	 *
	 * @return String A representation of the contents of the entire cache.
	 */
	public String toString() {
		String str = "";
	
		for(int row = 0; row < cache.length; row++) {
			for(int col = 0; col < cache[row].length; col++) 
				str += fmt3.format(row) + ": " + cache[row][col].toString() + "      ";
			str += "\n";
		}

		return str + "\n";
	}
}