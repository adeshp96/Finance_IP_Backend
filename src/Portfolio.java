import java.util.ArrayList;
import java.util.Comparator;

public class Portfolio implements Comparator{
	ArrayList <Pair<MutualFund, Float> > mutual_funds = new ArrayList<Pair<MutualFund, Float>>();
	Float returns, risk;
	@Override
	public String toString() {
		String output = "\nReturn: " + returns + " Risk: "+risk + " Returns/Risk: "+ returns/risk +";\n";
		for (Pair<MutualFund, Float> e: mutual_funds) {
			output += e.getL() + ": " + e.getR() + "; ";
		}
		return output;
	}
	@Override
	public int compare(Object o1, Object o2) {
		Portfolio p1 = (Portfolio) o1;
		Portfolio p2 = (Portfolio) o2;
		float v1 = p1.returns/p1.risk, v2 = p2.returns/p2.risk;
		return (int)(v2 - v1)*1000;
	}
	
}
