import numpy as np
from sys import argv
np.seterr(all='raise')
np.set_printoptions(suppress= True)

if len(argv) == 1:
	argv.append("17")

if argv[1] == "18":
	print "Loading Feb 18 data"
	nifty_file = "niftyFeb2018.csv"
	mf_file = "NAVHistoryReportFeb2018.txt"
	first_nifty_date = "01-Feb-2018"
	last_nifty_date = "28-Feb-2018"
	stats_file = "statsFeb18.txt"
	normal_dist_file = "normal_distFeb18.txt"
	mf_consider = "MF_to_consider18.txt"
	correlation_file = "correlations18.txt"
	details_file = "mfdetails18.txt"
elif argv[1] == "17":
	print "Loading 2017 data"
	nifty_file = "nifty2017.csv"
	mf_file = "NAVHistoryReport2017.txt"
	first_nifty_date = "02-Jan-2017"
	last_nifty_date = "29-Dec-2017"
	stats_file = "stats17.txt"
	normal_dist_file = "normal_dist17.txt"
	mf_consider = "MF_to_consider17.txt"
	correlation_file = "correlations17.txt"
	details_file = "mfdetails17.txt"
else:
	assert False


risk_free_return_rate = 6.47
minimum_entries_for_alpha = 5
mf_to_consider_count = 1000 #max 1000
assert mf_to_consider_count <= 1000
#store date of nav release for every MF
dates = {}
#Key is MF code and value is the entire MutualFund class object
mutual_funds = {}

#Key is date and value is NiftyEntry class object
nifties = {}

class MFEntry:
	def __init__(self, nav, repurchase_price, sale_price, date):
		self.nav = float(nav)
		self.repurchase_price = repurchase_price
		self.sale_price = sale_price
		self.date = date
	def __str__(self):
		return self.date + " @ " + str(self.nav)

class MutualFund:
	def __init__(self, code, name, nav, repurchase_price, sale_price, date):
		self.code = code
		self.name = name
		self.entries = []
		self.first_entry_date = None
		self.last_entry_date = None
		self.beta = None
		self.alpha = None
		self.std =  None
		self.mean = None
	def add_entry(self, nav, repurchase_price, sale_price, date):
		new_entry = MFEntry(nav, repurchase_price, sale_price, date)
		self.entries.append(new_entry)
	def __str__(self):
		output = self.code + " : " + self.name
		for entry in self.entries:
			output += '\n' + entry.__str__()
		return output

class NiftyEntry:
	def __init__(self, date, close):
		self.date = date
		self.close = close
	def __str__(self):
		return "Nifty on " + self.date + " @ " + self.close


def filter_mf():
	mf_to_consider = {}
	counter = 0
	with open(mf_consider) as fp:
		for line in fp:
			counter += 1
			if counter > mf_to_consider_count:
				break
			line = line.strip()
			mf_to_consider[line] = True
	keys = mutual_funds.keys()
	for mf_code in keys:
		if mf_code not in mf_to_consider:
			del mutual_funds[mf_code]

def add_entry_mutual_fund(code, name, nav, repurchase_price, sale_price, date):
	if nav == "N.A.":
		# Do NOT add this MF/Entry since its NAV is not available
		return
	if code not in mutual_funds:
		new_mf = MutualFund(code, name, nav, repurchase_price, sale_price, date)
		mutual_funds[code] = new_mf
	mutual_funds[code].add_entry(nav, repurchase_price, sale_price, date)


def add_entry_nifty(date, close):
	new_nifty = NiftyEntry(date, close)
	nifties[date] = new_nifty

def get_avg_return_of_nifty():
	nifty_close_sum = 0.0
	nifty_start_value = float(nifties[first_nifty_date].close)
	nifty_end_value = float(nifties[last_nifty_date].close)
	return 100 * ( ( nifty_end_value - nifty_start_value ) / nifty_start_value)

def get_betas():
	#Key is date value is nifty close price
	nifty_simple_dict = {}
	#key is mf code value is beta
	betas = {}
	for nifty_date in nifties:
		nifty_simple_dict[nifty_date] = float(nifties[nifty_date].close)
	nifty_variance = np.var(nifty_simple_dict.values())
	print "Nifty variance", nifty_variance
	for mf_code in mutual_funds:
		nifty_list_for_covariance = []
		entries_list_for_covariance = []
		for entry in mutual_funds[mf_code].entries:
			if entry.date in nifty_simple_dict:
				entries_list_for_covariance.append(float(entry.nav))
				nifty_list_for_covariance.append(float(nifty_simple_dict[entry.date]))
		assert len(nifty_list_for_covariance) == len(entries_list_for_covariance)
		if len(nifty_list_for_covariance) <= 1:
			continue
		betas[mf_code] = np.cov(entries_list_for_covariance, nifty_list_for_covariance)[0,1]/nifty_variance
		mutual_funds[mf_code].beta = betas[mf_code]
	return betas

def get_jensen_alpha(betas, mkt_avg):
	alphas = {}
	for mf_code in mutual_funds:
		if len(mutual_funds[mf_code].entries) < minimum_entries_for_alpha:
			continue
		if mf_code not in betas:
			continue
		expected_performance = risk_free_return_rate + betas[mf_code] * (mkt_avg - risk_free_return_rate)
		start_value = mutual_funds[mf_code].entries[0].nav + 1e-100
		end_value = mutual_funds[mf_code].entries[-1].nav
		actual_performance = 100 * ( (end_value - start_value) / start_value)
		alphas[mf_code] = expected_performance - actual_performance
		mutual_funds[mf_code].alpha = alphas[mf_code]
	return alphas


def compute_mf_stats():
	for mf_code in mutual_funds:
		l = []
		for entry in mutual_funds[mf_code].entries:
			l.append(entry.nav)
		mutual_funds[mf_code].mean = np.mean(l)
		mutual_funds[mf_code].std = np.std(l)



def get_correlation(mf_code1, mf_code2):
	mf1 = mutual_funds[mf_code1]
	mf2 = mutual_funds[mf_code2]
	if mf_code1 not in dates:
		d1 = []
		for entry in mf1.entries:
			d1.append(entry.date)
		dates[mf_code1] = d1
	if mf_code2 not in dates:
		d2 = []
		for entry in mf2.entries:
			d2.append(entry.date)
		dates[mf_code2] = d2
	d1 = dates[mf_code1]
	d2 = dates[mf_code2]
	if len(d1) <= 1 or len(d2) <= 1:
		return None
	common_dates = set(d1).intersection(set(d2))
	if len(common_dates) <= 1:
		return None
	nav1 = []
	nav2 = []
	for entry in mf1.entries:
		if entry.date in common_dates:
			nav1.append(entry.nav)
	for entry in mf2.entries:
		if entry.date in common_dates:
			nav2.append(entry.nav)
	try:
		return np.corrcoef(nav1, nav2)[0, 1]
	except FloatingPointError:
		#happens if one of them is always constant
		return None

with open(mf_file) as fp:
	counter = 0
	invalid_counter = 0
	fp.readline() #Read the headings line
	for line in fp:
		line = line.strip()
		counter += 1
		if counter % 500000 == 0:
			print counter, invalid_counter
		values = line.split(";")
		if len(values) != 6:
			invalid_counter += 1
			continue
		else:
			add_entry_mutual_fund(values[0], values[1], values[2], values[3], values[4], values[5])


with open(nifty_file) as fp:
	fp.readline() #Read the headings line
	for line in fp:
		line = line.strip()
		values = line.split(",")
		assert len(values) == 7
		add_entry_nifty(values[0], values[4])

nifty_avg = get_avg_return_of_nifty()
betas = get_betas()
alphas = get_jensen_alpha(betas, nifty_avg)

max_alpha = -1e100
min_alpha = 1e100
min_alpha_mf = None
max_alpha_mf = None
for mf_code in mutual_funds:
	alpha = mutual_funds[mf_code].alpha
	if alpha is None:
		continue
	if alpha > max_alpha:
		max_alpha = alpha
		max_alpha_mf = mf_code
	if alpha < min_alpha:
		min_alpha = alpha
		min_alpha_mf = mf_code

print "max_alpha:",max_alpha,"max_alpha_mf:", mutual_funds[max_alpha_mf].name
print "min_alpha:",min_alpha,"min_alpha_mf:", mutual_funds[min_alpha_mf].name

compute_mf_stats()

filter_mf()
print len(mutual_funds)

with open(stats_file, "w") as fp:
	for mf_code in mutual_funds:
		mf = mutual_funds[mf_code]
		fp.write(mf_code + " " + str('{:f}'.format(mf.mean)) + " " + str('{:f}'.format(mf.std)) +" "+ str('{:f}'.format(mf.alpha)) + " " +str('{:f}'.format(mf.beta)) + '\n')

with open(details_file, "w") as fp:
	for mf_code in mutual_funds:
		mf = mutual_funds[mf_code]
		fp.write(mf.code + ";"+mf.name+"\n")



# l = []
# for mf_code in mutual_funds:
# 	l.append((len(mutual_funds[mf_code].entries),mf_code))

# l.sort(reverse = True)
# with open("MF_to_consider18.txt", "w") as fp:
# 	for mf in range(1000):
# 		fp.write(l[mf][1] + '\n')
# 	fp.flush()
# 	print l[mf][0]
# 	print "Written"



# correlation_dict = {}
# counter = 0
# for mf_code1 in mutual_funds:
# 	counter += 1
# 	if counter % 10 == 0:
# 		print counter
# 	for mf_code2 in mutual_funds:
# 		if (mf_code2, mf_code1) in correlation_dict:
# 			continue
# 		value = get_correlation(mf_code1, mf_code2)
# 		if value is not None:
# 			correlation_dict[(mf_code1, mf_code2)] = value
# with open(correlation_file, 'w') as fp:
# 	for e in correlation_dict:
# 		fp.write(e[0] + ' ' + e[1] + ' ' + str('{:f}'.format(correlation_dict[e])) + '\n')