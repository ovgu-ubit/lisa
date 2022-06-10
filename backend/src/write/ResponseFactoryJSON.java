package write;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import model.Title;
import model.Title.Copy;
import model.Title.Copy.Statistic;

public abstract class ResponseFactoryJSON {

	public static String getJSON(List<Title> titles) {
		JSONArray res = new JSONArray();
		for (Title title : titles) {
		
			JSONObject json = new JSONObject();
			json.put("query", title.query);
			if (!title.ppn.isEmpty()) {
				json.put("ppn", title.ppn.replaceAll("\"", "\\\""));
				json.put("material_code", title.material_code.replaceAll("\"", "\\\""));
				json.put("material", title.material.replaceAll("\"", "\\\""));
				json.put("language_text", title.language_text.replaceAll("\"", "\\\""));
				//json.put("language_orig", title.language_orig.replaceAll("\"", "\\\""));
				json.put("year_of_creation", title.year_of_creation.replaceAll("\"", "\\\""));
				json.put("title", title.title.replaceAll("\"", "\\\""));
				json.put("volume", title.volume.replaceAll("\"", "\\\""));
				//json.put("author_ppn", title.author_ppn.replaceAll("\"", "\\\""));
				json.put("author", title.author.replaceAll("\"", "\\\""));
				//json.put("co_authors_ppn", title.co_authors_ppn.replaceAll("\"", "\\\""));
				json.put("co_authors", title.co_authors.replaceAll("\"", "\\\""));
				json.put("edition", title.edition.replaceAll("\"", "\\\""));
				json.put("publisher", title.publisher.replaceAll("\"", "\\\""));
				//json.put("publisher_location", title.publisher_location.replaceAll("\"", "\\\""));
				//json.put("ddc", title.ddc.replaceAll("\"", "\\\""));
				json.put("bkl", title.bkl.replaceAll("\"", "\\\""));
				json.put("local_expansion", title.local_expansion.replaceAll("\"", "\\\""));
				json.put("classification", title.classification.replaceAll("\"", "\\\""));
				json.put("link", title.link.replaceAll("\"", "\\\""));
				json.put("edition", title.edition.replaceAll("\"", "\\\""));
				json.put("type", title.type.replaceAll("\"", "\\\""));
				json.put("publisher_location", title.publisher_location.replaceAll("\"", "\\\""));
				json.put("doi", title.doi.replaceAll("\"", "\\\""));
				json.put("superPPN", title.superPPN.replaceAll("\"", "\\\""));
				json.put("greater_entity", title.greaterEntity.replaceAll("\"", "\\\""));
				json.put("greater_entity_issn", title.greaterEntityISSN.replaceAll("\"", "\\\""));
				json.put("greater_entity_year", title.greaterEntityYear.replaceAll("\"", "\\\""));
				JSONArray array_copies = new JSONArray();
				for (Copy c : title.copies) {
					JSONObject json_copy = new JSONObject();
					json_copy.put("loan_date", c.loan_date.replaceAll("\"", "\\\""));
					json_copy.put("status", c.status.replaceAll("\"", "\\\""));
					json_copy.put("epn", c.epn.replaceAll("\"", "\\\""));
					json_copy.put("selection_key", c.selection_key.replaceAll("\"", "\\\""));
					json_copy.put("signature", c.signature.replaceAll("\"", "\\\""));
					json_copy.put("location", c.location.replaceAll("\"", "\\\""));
					json_copy.put("loan_indicator", c.loan_indicator.replaceAll("\"", "\\\""));
					json_copy.put("barcode", c.barcode.replaceAll("\"", "\\\""));
					json_copy.put("local_sys", c.local_sys.replaceAll("\"", "\\\""));
					if (!c.remark_intern.isEmpty() && !c.remark.isEmpty()) json_copy.put("remark", c.remark.replaceAll("\"", "\\\"")+" | "+c.remark_intern.replaceAll("\"", "\\\""));
					else if (c.remark.isEmpty()) json_copy.put("remark", c.remark_intern.replaceAll("\"", "\\\""));
					else if (c.remark_intern.isEmpty()) json_copy.put("remark", c.remark.replaceAll("\"", "\\\""));
					JSONArray array_stats = new JSONArray();
					for (Statistic s : c.stats) {
						JSONObject json_stat = new JSONObject();
						json_stat.put("year", s.year);
						json_stat.put("num_loans", s.num_loans);
						json_stat.put("num_reserv", s.num_reserv);
						//json_stat.put("num_req", s.num_req);
						array_stats.put(json_stat);
					}
					json_copy.put("stats", array_stats);
					array_copies.put(json_copy);
				}
				json.put("copies", array_copies);
			} else json.put("errorcode", 400); // in case no title has been retrieved, return an error
			res.put(json);
		}
		return res.toString();
	}

}
