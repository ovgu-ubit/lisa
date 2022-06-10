package model.predicates;

import java.util.function.Predicate;

import model.Title;
import model.Title.Copy;

/**
 * predicate to identify titles with a certain range of copies
 * @author sbosse
 *
 */
public class MultiCopy implements Predicate<Title>{

	
	int min_ex;
	int max_ex;
	String location;
	String sls;
	
	/**
	 * 
	 * @param min_ex minimum number of copies in location and with selection key sls
	 * @param max_ex maximum number of copies in location and with selection key sls
	 * @param location prefix
	 * @param sls prefix
	 */
	public MultiCopy(int min_ex, int max_ex, String location, String sls) {
		this.min_ex = min_ex;
		this.max_ex = max_ex;
		this.location = location;
		this.sls = sls;
	}
	
	@Override
	public boolean test(Title t) {
		int res = 0;
		for (Copy c : t.copies) {
			if (c.location.startsWith(location) && c.selection_key.startsWith(sls)) res++;
		}
		return res>=min_ex && res<=max_ex;
	}

}
