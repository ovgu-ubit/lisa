package model.predicates;

import java.util.function.Predicate;

import model.Title;

/**
 * predicate to test if a title belongs to more than one classification
 * @author sbosse
 *
 */
public class MultiClass implements Predicate<Title>{

	@Override
	public boolean test(Title arg0) {
		return arg0.classification.contains("|");
	}

}
