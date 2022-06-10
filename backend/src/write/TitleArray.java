package write;

import java.util.ArrayList;
import java.util.List;

import model.Title;

/**
 * TitleWriter that stores an array of Title objects for processing
 * @author sbosse
 *
 */
public class TitleArray implements TitleWriter{

	List<Title> titles;
	boolean superInfos;
	
	public TitleArray(boolean superInfos) {
		this.superInfos = superInfos;
	}
	
	@Override
	public void addTitle(Title title) {
		this.titles.add(title);
	}

	@Override
	public void close() {
		
	}

	@Override
	public void init(String query) {
		titles = new ArrayList<Title>();
	}
	
	public List<Title> getTitles() {
		return titles;
	}

	@Override
	public boolean superInfos() {
		return superInfos;
	}
	


}
