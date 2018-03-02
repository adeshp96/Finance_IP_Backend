import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.NormalDistribution;

public class Manager {
	static Map<String, MutualFund> mutual_funds_map = new TreeMap <String, MutualFund>();
	static Map<String, Map <String, Float>> mutual_funds_correlations = new TreeMap<String, Map <String, Float>>();
	public static void LoadDetails() throws IOException {
		FileReader details = null, stats = null, correlations = null;
        try
        {
            details = new FileReader("src/resources/mfdetails17.txt");
            stats = new FileReader("src/resources/stats17.txt");
            correlations = new FileReader("src/resources/correlations17.txt");
        }
        catch (FileNotFoundException fe)
        {
            System.out.println("Required files not found!");
            fe.printStackTrace();
            System.exit(1);
        }
        BufferedReader bufferedReader = new BufferedReader(details);
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			String code = line.split(";")[0];
			String name = line.split(";")[1];
			MutualFund mf = new MutualFund(code, name);
			mutual_funds_map.put(code, mf);
		}
		details.close();
		bufferedReader = new BufferedReader(stats);
		while ((line = bufferedReader.readLine()) != null) {
			String code = line.split(" ")[0];
			Float mean = Float.parseFloat(line.split(" ")[1]);
			Float std = Float.parseFloat(line.split(" ")[2]);
			Float alpha = Float.parseFloat(line.split(" ")[3]);
			Float beta = Float.parseFloat(line.split(" ")[4]);
			mutual_funds_map.get(code).setStats(mean, std, alpha, beta);
		}
		stats.close();
		bufferedReader = new BufferedReader(correlations);
		while ((line = bufferedReader.readLine()) != null) {
			String code1 = line.split(" ")[0];
			String code2 = line.split(" ")[1];
			Float corr = Float.parseFloat(line.split(" ")[2]);
			if(mutual_funds_correlations.containsKey(code1) == false)
				mutual_funds_correlations.put(code1, new TreeMap<String, Float>());
			if(mutual_funds_correlations.containsKey(code2) == false)
				mutual_funds_correlations.put(code2, new TreeMap<String, Float>());
			mutual_funds_correlations.get(code1).put(code2, corr);
			mutual_funds_correlations.get(code2).put(code1, corr);
		}
		correlations.close();
		System.out.println(mutual_funds_map.size() + " mutual funds loaded successfully.");
	}
	static ArrayList<Portfolio> getPortfolios(double returns, double risk) throws Exception{
		ArrayList<Portfolio> output = new ArrayList <Portfolio>();
		if(risk < 0 || risk > 1 || returns < 0 || returns > 1)
			throw new Exception();
		returns += 1;
		double probability = 1 - risk;
		for(String code: mutual_funds_map.keySet()) {
			MutualFund mf = mutual_funds_map.get(code);
			NormalDistribution dist = null;
			try {
				dist = new NormalDistribution(mf.mean,mf.std);
			}
			catch(Exception e) {
				continue;
			}
			double required_probability = 1 - dist.cumulativeProbability(returns);
			if(required_probability >= probability) {
				Portfolio p = new Portfolio();
				p.mutual_funds.add(mf);
				output.add(p);
			}
		}
		return output;
	}
}
