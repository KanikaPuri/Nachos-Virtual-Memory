Readme.txt

Partners:
Daniel Burgoyne
Kanika Puri

To compile the files, use the make command in the Nachos-Java directory.
(e.g. cd Nachos-Java; make) 

Tests are provided in conf/PA3/ and have the page size as part of the file name.
Your current working directory must be that of Nachos-Java so that it can find
the test directory. The tests prefixed with Chk were primarily used for testing
during development. Specifically, ChkMulti shows that the new configuration
parameter correctly runs multiple programs, ChkMap shows that different
processes can run concurrently and produce the correct results (matmult returns
7220 and sort returns 0). ChkReject shows that programs which are too large will
be rejected. ChkRecur runs a recursive function that exercises the stack and
generates page faults. The other tests were used for performance comparison. To
run any tests make sure your environment variables are set correctly
(specifically the CLASSPATH variable) and then run nachos giving it the name of
the conf file for the test you'd like to run.

Performance tests:

Tests prefixed with Performance and postfixed with the page size were used in
comparing the performance of the system. They are all long running tests which
run a series of programs to fully stress the system. More about these will be
discussed in Design.txt.

There is a test script called test.bash which simply runs each of the
performance tests in order from smallest page size to largest. NOTE: From
experience, it takes roughly 2.5 minutes to run on a fairly powerful machine.
Your results may vary.

Added configuration parameters:

The only configuration parameters added are those that have been added as
specified by the programming assignments. In particular processor.pageSize is
new for this assignment and specifies the size of the pages in number of bytes.
