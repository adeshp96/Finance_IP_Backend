import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.distribution.NormalDistribution;

public class Manager {
	static Map<String, MutualFund> mutual_funds_map = new TreeMap <String, MutualFund>();
	static Map<String, Map <String, Float>> mutual_funds_correlations = new TreeMap<String, Map <String, Float>>();
	static double relaxation = 0.02;
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
//	static private Double getRiskFromStd(double std) {
//		NormalDistribution dist = new NormalDistribution(1, );
//	}
	
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
				String global_min_risk_mf = null;
				float global_w1 = 1.0f, global_w2 = 0.0f;
				float std1 = mf.std;
				float global_minimum_risk = std1*std1;
				if(mutual_funds_correlations.containsKey(code)) {
					for(String code2: mutual_funds_map.keySet()) {
						if (code.equals(code2))
							continue;
						if (mutual_funds_correlations.get(code).containsKey(code2) == false)
							continue;
						MutualFund mf2 = mutual_funds_map.get(code2);
						float std2 = mf2.std;
						float corr = mutual_funds_correlations.get(code).get(code2);
						float best_w1 = 1, best_w2 = 0;
						float min_risk = std1*std1*best_w1*best_w1 + std2*std2*best_w2*best_w2 + 2*best_w1*best_w2*corr*std1*std2;
						for(float w1 = 0; w1 <= 1; w1 += 0.1) {
							float w2 = 1 - w1;
							float curr_risk = std1*std1*w1*w1 + std2*std2*w2*w2 + 2*w1*w2*corr*std1*std2;
							if(curr_risk < min_risk) {
								min_risk = curr_risk;
								best_w1 = w1;
								best_w2 = w2;
							}
						}
						if (min_risk < global_minimum_risk && best_w2 > 0 && best_w1 > 0 && mf.mean*best_w1 + mutual_funds_map.get(code2).mean*best_w2 + relaxation >= returns) {
							global_minimum_risk = min_risk;
							global_min_risk_mf = code2;
							global_w1 = best_w1;
							global_w2 = best_w2;
						}
					}
					
				}
				Portfolio p = new Portfolio();
				p.mutual_funds.add(new Pair(mf, global_w1));
				if(global_min_risk_mf != null)
					p.mutual_funds.add(new Pair(mutual_funds_map.get(global_min_risk_mf), global_w2));
				p.returns = mf.mean*global_w1 + mutual_funds_map.get(global_min_risk_mf).mean*global_w2;
				p.risk = global_minimum_risk;
				output.add(p);
			}
		}
		output.sort(new Portfolio());
		return output;
	}
}
