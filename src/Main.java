import java.util.ArrayList;

public class Main {
	
	public static void main(String[] args) {
		try {
			Manager.LoadDetails();
			ArrayList<Portfolio> output = Manager.getPortfolios(0.2,0.6);
			System.out.println(output);
			System.out.println(output.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
