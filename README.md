# Computer-Network-STP-UDP
A system to implement the STP over UDP, which is a reliable transmission including most features of TCP e.g. the mechanism of re-transmission due to timeout and error detection etc. The system also has a module to simulate the package drop and delay as well as bit flip during the transmission. A report is to reflect the reliability of the implemented STP over UDP.

Instructions to run the program
1. Ensure the JRE and JDK are available on the target computer.
2. Compile all *.java to *.class under the proj1 folder.
3. Run Sender using command "java Sender receiver_host_ip receiver_port file.pdf MWS MSS gamma pDrop
   pDuplicate pCorrupt pOrder maxOrder pDelay maxDelay seed" where the parameters are the sliding window size, Maximum segment size and        probability of exception during the transmission.
4. Run Receiver using "java Receiver receiver_port file_r.pdf"
5. After file transmission is done the log files will be generated automatically under the target directory.

The proj1 folder includes the source codes and the report for the project.
And the project spec is located under the root of project directory.
