import java.util.ArrayList;

public class Main {
	
	public static void main(String[] args) {
		try {
			Manager.LoadDetails();
			ArrayList<Portfolio> output = Manager.getPortfolios(0.2,0.4);
			System.out.println(output);
			System.out.println("Output size: " +output.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
