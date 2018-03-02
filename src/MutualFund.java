
public class MutualFund {
	String code;
	String name;
	Float mean, std, alpha, beta;
	
	public MutualFund(String code, String name) {
		super();
		this.code = code;
		this.name = name;
	}
	public void setStats(Float mean, Float std, Float alpha, Float beta) {
		this.mean = mean;
		this.std = std;
		this.alpha = alpha;
		this.beta = beta;
	}
	@Override
	public String toString() {
		return "MutualFund [code=" + code + ", name=" + name + ", mean=" + mean + ", std=" + std + "]";
	}

	
}
