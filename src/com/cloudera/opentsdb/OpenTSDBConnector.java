package com.cloudera.opentsdb.connector;

import org.apache.hadoopts.data.series.Messreihe;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * Created by kamir on 24.06.17.
 * <p>
 * https://hc.apache.org/httpcomponents-client-ga/examples.html
 * <p>
 * Simple example for data ingestion to OpenTSDB using the REST-API.
 */
public class OpenTSDBConnector {

    public static final String VARIABLE_TEMPLATE_METRIC = "___METRIC___";
    public static final String VARIABLE_TEMPLATE_VALUE = "___VALUE___";
    public static final String VARIABLE_TEMPLATE_TS = "___TS___";
    public static final String VARIABLE_TEMPLATE_TAG_KEY = "___TAG_KEY___";
    public static final String VARIABLE_TEMPLATE_TAG_VALUE = "___TAG_VALUE___";
    // URL url = new URL("http://cdsw-mk8-1.vpc.cloudera.com:4343/api/put");
    URL url = null;
    String eventTemplate = null;

    String OPENTSDB_HOST = "cc-poc-mk-1.gce.cloudera.com";
    // String OPENTSDB_HOST = "127.0.0.1";

    public OpenTSDBConnector() throws MalformedURLException {
        url = new URL("http://" + OPENTSDB_HOST + ":4242/api/put");
        eventTemplate = initTSToSend();
    }

    public OpenTSDBConnector(String host) throws MalformedURLException {
        OPENTSDB_HOST = host;
        url = new URL("http://" + host + ":4242/api/put");
        eventTemplate = initTSToSend();
    }

    public static void main(String[] args) throws Exception {


        stdlib.StdRandom.initRandomGen(1);


        // The URL of OpenTSDB Server is known by this component.
        OpenTSDBConnector connector = new OpenTSDBConnector();


        long t0 = System.currentTimeMillis();


        // TODO : Write Benchmark

        /**
         * Put a simple event to OpenTSDB.

         // THE BULK LOAD IS NOT WORKING YET (need a template for this) ...
         int i = 0;
         while ( i < 2 ) {

         HttpURLConnection httpCon = (HttpURLConnection) connector.url.openConnection();
         httpCon.setDoOutput(true);
         httpCon.setRequestMethod("POST");

         OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());

         i++;

         OpenTSDBEvent e = new OpenTSDBEvent();
         e.metric = "sys.cpu.idle";
         e.tags.put("server", "2");
         e.tags.put("rack", "1");
         e.tags.put("ds", "3");
         e.timestamp = "" + (t0 + (i * 1000));
         e.value = (Math.random() * 5009) + "";

         String pl = e.toJSON();

         System.out.println(pl);

         out.write(pl);

         out.close();

         InputStream ins = httpCon.getInputStream();

         BufferedReader bins = new BufferedReader(new InputStreamReader(ins));

         while (bins.ready())
         System.out.println(" RESPONSE: " + bins.readLine());


         }
         */


        /**
         * Put a set of noisy "Messreihe" to OpenTSDB with current time stamp.


         Messreihe mr1 = Messreihe.getGaussianDistribution( 100 );
         mr1.setLabel( "demo1 s=1,distr=gauss,run=1" );
         Messreihe mr2 = Messreihe.getGaussianDistribution( 100, 1000, 10 );
         mr2.setLabel( "demo1 s=2,distr=gauss,run=1" );
         Messreihe mr3 = Messreihe.getGaussianDistribution( 100, 100, 2000 );
         mr3.setLabel( "demo1 s=3,distr=gauss,run=1" );

         storeMessreihe( mr1, connector );
         storeMessreihe( mr2, connector );
         storeMessreihe( mr3, connector );
         */


        /**
         *
         */

        /**
         * NEEDS BETTER SCALING
         */
        Messreihe mr10 = Messreihe.getParetoDistribution(1000, 10000);
        mr10.setLabel("demo1 distr=pareto,run=1");

        Messreihe mr20 = Messreihe.getExpDistribution(1000, 10000);
        mr20.setLabel("demo1 distr=exp,run=1");

        Messreihe mr30 = Messreihe.getGaussianDistribution(1000, 10000, 100);
        mr30.setLabel("demo1 distr=gauss,run=1");

        Vector<Messreihe> bucketData = new Vector<Messreihe>();
        bucketData.add(mr10);
        bucketData.add(mr20);
        bucketData.add(mr30);

        storeBucketData(bucketData, connector, System.currentTimeMillis());


    }

    protected static void storeBucketData(Vector<Messreihe> bucketData, OpenTSDBConnector connector, long l) throws Exception {
        for (Messreihe mr : bucketData) {
            storeMessreihe(mr, connector, l);
        }

    }

    private static void storeListOfEventsFromMessreihe(List<OpenTSDBEvent> events, OpenTSDBConnector connector) throws Exception {

        StringBuffer sb = new StringBuffer();
        sb.append("[");

        for (OpenTSDBEvent e : events) {

            sb.append(e.toJSON() + ",");

        }

        String all = sb.toString();
        String cont = all.substring(0, all.length() - 1);
        all = cont + "]";


        HttpURLConnection httpCon = (HttpURLConnection) connector.url.openConnection();
        httpCon.setDoOutput(true);
        httpCon.setRequestMethod("POST");

        OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());

        out.write(all);

        //System.out.println( all );

        out.close();

        InputStream ins = httpCon.getInputStream();

        BufferedReader bins = new BufferedReader(new InputStreamReader(ins));

        while (bins.ready())
            System.out.println(" RESPONSE: " + bins.readLine());


    }

    protected static void storeMessreihe(Messreihe row, OpenTSDBConnector connector, long offset) throws Exception {

        // We expect the metric as String in this way.

        List<OpenTSDBEvent> events = getEventsFrom(row, offset);


        int z = events.size();

        int batch = 0;
        int volume = 50;

        List<OpenTSDBEvent> l;
        int von = 0;
        int bis = volume;

        while (batch * volume < z) {
            von = batch * volume;
            bis = (batch + 1) * volume;
            l = events.subList(von, bis);
            batch++;
            storeListOfEventsFromMessreihe(l, connector);
        }
        bis = z;
        l = events.subList(von, bis);
        batch++;
        storeListOfEventsFromMessreihe(l, connector);


        System.out.println("zBatches: " + batch + " => " + row.getLabel());




        /*
        for ( OpenTSDBEvent e : events ) {

            //System.out.println( ">>>" + e.toJSON() );

            HttpURLConnection httpCon = (HttpURLConnection) connector.url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("POST");

            OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());

            String pl = e.toJSON();

            System.out.println(pl);

            out.write(pl);

            out.close();

            InputStream ins = httpCon.getInputStream();

            BufferedReader bins = new BufferedReader(new InputStreamReader(ins));

            while (bins.ready())
                System.out.println(" RESPONSE: " + bins.readLine());



        }

        */

//            InputStream ins = httpCon.getInputStream();
//            BufferedReader bins = new BufferedReader(new InputStreamReader(ins));

//            while (bins.ready())
//                System.out.println(bins.readLine());

    }

    protected static void storeMessreihe(Messreihe row, OpenTSDBConnector connector) throws Exception {

        long t0 = System.currentTimeMillis();

        storeMessreihe(row, connector, t0);

    }

    //
    // We need bulk load to OpenTSDB:
    //
    // tsd.http.request.enable_chunked=true
    // tsd.mode=ro => READ ONLY !!! needs to be set to RW
    //
    // docker container ls
    // docker exec -it 6b5162c943bd /bin/bash
    // vi /etc/opentsdb/opentsdb.conf
    //
    private static List<OpenTSDBEvent> getEventsFrom(Messreihe row, long offset) throws CloneNotSupportedException {

        double resolution = 1000.0;

        // System.out.print( row );

        List<OpenTSDBEvent> liste = new ArrayList<OpenTSDBEvent>();

        double[][] DATA = row.getData();

        // Iterate over all data points ...
        int i = 0;

        for (i = 0; i < DATA[0].length; i++) {

            OpenTSDBEvent e = new OpenTSDBEvent(); // TEMPLATE OBJECT

            // "sys.cpu.idle server=2,rack=1,ds=1";
            String name = row.getLabel();
            // String name = "sys.cpu.idle server=2,rack=1,ds=1";

//            System.out.println("[LABEL] " + row.getLabel() );

            String[] PARTS = name.split(" ");
            String metric = PARTS[0];
            e.metric = metric;

            String[] TAGS = PARTS[1].split(",");
            for (String tag : TAGS) {
                String[] KV = tag.split("=");
                e.tags.put(KV[0], KV[1]);
            }

            e.timestamp = "" + (long) (offset + (long) (resolution * DATA[0][i]));
            e.value = "" + DATA[1][i];

            liste.add(e);

            i++;

        }

        return liste;

    }


    private static String initTSToSend() {

        String s = null;

        try {

            s = new String(Files.readAllBytes(Paths.get("/GITHUB/cuda-tsa/src/main/resources/datapoint")));
            System.out.println(s);

            System.out.println("\n***************************************************");


        } catch (IOException e) {
            e.printStackTrace();
        }

        return s;

    }
}
