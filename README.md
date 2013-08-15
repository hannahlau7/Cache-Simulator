Hannah Lau, hlau7, 902791141
Cache.java, version 6.0

==============
  Simulator
==============

To use the cache simulator, compile the .java file using the command: 
	
	javac Cache.java

The simulator can either run and simulate a trace file or run an experiment on a trace file. To run the simulator on a trace file, use the command:
	
	java Cache trace_file.txt -c # -b # -s # -p #

	Ex. To simulate a cache reading from bzip2_trace.txt with 2^10 bytes storage, 2^5 bytes per block, 2^s associativity, and 4 blocks prefetching, input:

	java Cache bzip2_trace.txt -c 10 -b 5 -s 1 -p 4

Where trace_file.txt is the corresponding trace file and the #'s are the parameters to the cache. To run an experiment on a trace file, use the command:
	
	java Cache trace_file.txt #

	Ex. To run an experiment to find the best cache stats on the bzip2_trace.txt file with a total cache size of 48KB, input:

	java Cache bzip2_trace.txt 48

Where trace_file.txt is the corresponding trace file and the # is the size of the cache in KB. 

The cache simulator also comes with a debugging feature. To view the debugging feature, I recommend you run the simulator on a small file such as the exp1.txt and then follow the program's prompting. 


==============
  Validation
==============

I validated my cache using the exp1.txt trace file that I created. I worked with Aziz Somani and compared results. After some work, our outputs matched. Here are some of the results for the exp1.txt trace file.

Cache Dimensions
-----------------
c: 5 b: 2 s: 0 p: 1

Access to cache: 7
Reads: 5
Read misses: 2
Writes: 2
Write misses: 1
Write backs in bytes: 4
Total bytes transferred: 20
Total blocks prefetced: 2
Total misses: 3
Miss rate: 0.400
Total number of bits of cache storage: 488

EMAT: 22.000

Cache Dimensions
-----------------
c: 6 b: 5 s: 1 p: 4

Access to cache: 7
Reads: 5
Read misses: 4
Writes: 2
Write misses: 2
Write backs in bytes: 0
Total bytes transferred: 608
Total blocks prefetced: 15
Total misses: 6
Miss rate: 0.800
Total number of bits of cache storage: 570

EMAT: 42.200

Cache Dimensions
-----------------
c: 10 b: 4 s: 2 p: 10

Access to cache: 7
Reads: 5
Read misses: 2
Writes: 2
Write misses: 1
Write backs in bytes: 0
Total bytes transferred: 352
Total blocks prefetced: 20
Total misses: 3
Miss rate: 0.400
Total number of bits of cache storage: 9856

EMAT: 22.400


==============
  Experiments
==============

48KB Total Space
-------------------
Results for file bzip2_trace.txt
Cache stats: -c 15 -b 10 -s 0 -p 3
EMAT: 2.003704526931911

Results for file cg_trace.txt
Cache stats: -c 15 -b 9 -s 1 -p 4
EMAT: 2.4204571243313344

Results for file gcc_trace.txt
Cache stats: -c 15 -b 5 -s 1 -p 14
EMAT: 2.570835952231301

Results for file go_trace.txt
Cache stats: -c 15 -b 4 -s 0 -p 49
EMAT: 2.212825631465197

Results for file mcf_trace.txt
Cache stats: -c 15 -b 12 -s 2 -p 0
EMAT: 6.399974671681674

Results for file parser_trace.txt
Cache stats: -c 15 -b 7 -s 2 -p 1
EMAT: 3.5142572492265742


192KB Total Space
-------------------
Results for file bzip2_trace.txt
Cache stats: -c 17 -b 16 -s 0 -p 0
EMAT: 2.0007409053863823

Results for file cg_trace.txt
Cache stats: -c 17 -b 11 -s 1 -p 6
EMAT: 2.2270168044523695

Results for file gcc_trace.txt
Cache stats: -c 17 -b 9 -s 1 -p 11
EMAT: 2.2644248900062856

Results for file go_trace.txt
Cache stats: -c 17 -b 6 -s 0 -p 49
EMAT: 2.028871694191015

Results for file mcf_trace.txt
Cache stats: -c 17 -b 13 -s 2 -p 1
EMAT: 2.8882033357395236

Results for file parser_trace.txt
Cache stats: -c 17 -b 8 -s 1 -p 3
EMAT: 2.7446751612395786


==============
   Credits
==============

Questions or concerns, email me at hlau7@gatech.edu.

Cheers!