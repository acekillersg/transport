package edu.illinois.adsc.transport.InputSimulator;

import edu.illinois.adsc.transport.Config;
import edu.illinois.adsc.transport.generated.Matrix;
import edu.illinois.adsc.transport.generated.QueryService;
import edu.illinois.adsc.transport.generated.StationUpdate;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import java.io.*;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by robert on 10/12/15.
 */
public class InputSimulatorFromSingleFile {




    String fileName;

    TTransport transport;
    TProtocol protocol;
    QueryService.Client client;

    public InputSimulatorFromSingleFile(String file) {
        fileName = file;
    }

    boolean connectToThriftServer() {
        transport = new TSocket("localhost", Config.thriftPort);


        try{
            transport.open();

            protocol = new TBinaryProtocol(transport);

            client = new QueryService.Client(protocol);

            System.out.format("connected to thrift server %s:%d","localhost",Config.thriftPort);
            return true;

        } catch (TTransportException e) {
            e.printStackTrace();
            return false;
        }
    }

    void simulateUpdate() {
        try{
            FileInputStream fileInputStream = new FileInputStream(fileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fileInputStream));

            //skip the header
            bufferedReader.readLine();

            String line;

            while((line = bufferedReader.readLine())!=null) {
                Pattern p = Pattern.compile( "([0-9]+-[0-9]+-[0-9]+,[0-9]+:[0-9]+):[0-0]+,([0-9]+),([0-9]+)" );
                Matcher m = p.matcher(line);
                if(m.find()) {
                    String station = m.group(2);
                    String time = m.group(1);
                    String value = m.group(3);

                    StationUpdate stationUpdate = new StationUpdate();
                    stationUpdate.stationId =  station;
                    stationUpdate.timeStamp = time;

                    Matrix matrix = new Matrix();
                    matrix.rows = 1;
                    matrix.columns = 1;
                    matrix.data = new Vector<Double>(1);
                    matrix.data.add(Double.parseDouble(value));

                    stationUpdate.updateMatrix = matrix;

                    try {
                        client.pushUpdate(stationUpdate);
                        Thread.sleep(10000);
                    } catch (TException e ) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }



                    System.out.format("station:%s,time:%s,value:%s\n",station,time,value);
                }
            }

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    void close() {
        transport.close();
    }

    public static void main(String[] args) {
        if(args.length==0) {
            System.err.println("Input file is not specified!");
            return;
        }

        InputSimulatorFromSingleFile inputSimulatorFromSingleFile = new InputSimulatorFromSingleFile(args[0]);

        inputSimulatorFromSingleFile.connectToThriftServer();

        inputSimulatorFromSingleFile.simulateUpdate();

        inputSimulatorFromSingleFile.close();

        System.out.println("The input file is exhausted!");
    }
}
