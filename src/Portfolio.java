import java.util.ArrayList;

public class Portfolio {
	ArrayList <Pair<MutualFund, Float> > mutual_funds = new ArrayList<Pair<MutualFund, Float>>();
	Float returns, risk;
	@Override
	public String toString() {
		String output = "\nReturn: " + returns + "; Risk: "+risk + "\n";
		for (Pair<MutualFund, Float> e: mutual_funds) {
			output += e.getL() + ": " + e.getR() + "; ";
		}
		return output;
	}
	
}
