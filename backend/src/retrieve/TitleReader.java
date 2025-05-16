package retrieve;

import java.util.List;

import model.Title;

public interface TitleReader {
	public boolean retrieve(String searchStringCQL, int max_results, boolean write, int[] stat_years,
			boolean orderInfos, boolean GVK_infos, boolean classInfosFromSuperTitle, boolean distinct)
			throws QueryErrorException;

	public boolean retrieve(String searchStringCQL, int max_results, int pos, boolean write, int[] stat_years,
			boolean orderInfos, boolean GVK_infos, boolean classInfosFromSuperTitle, boolean distinct)
			throws QueryErrorException;

	public void close();

	public String write(String prefix);

	public String write();

	public void retrieveFromEPNs(List<Title> titles, boolean orderInfos) throws QueryErrorException;

	public void retrieveFromTitles(List<Title> titles, boolean orderInfos) throws QueryErrorException;
}
