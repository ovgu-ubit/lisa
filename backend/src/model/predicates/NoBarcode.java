package model.predicates;

import java.util.function.Predicate;

import model.Title;
import model.Title.Copy;


public class NoBarcode implements Predicate<Title>{

	

	public NoBarcode() {
	}
	
	@Override
	public boolean test(Title t) {
		for (Copy c : t.copies) {
			if ((c.loan_indicator.startsWith("u") || c.loan_indicator.startsWith("c")) && c.barcode.isEmpty()) return true;
		}
		return false;
	}

}
