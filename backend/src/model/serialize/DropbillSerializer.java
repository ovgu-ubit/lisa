package model.serialize;

import java.lang.reflect.Type;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import model.Title;
import model.Title.Copy;
import model.Title.Copy.Statistic;

public class DropbillSerializer implements JsonSerializer<Title> {

	@Override
	public JsonElement serialize(Title t, Type type, JsonSerializationContext jsonSerializationContext) {
		JsonObject title = new JsonObject();
		title.addProperty("query", t.query);
		if (t.ppn.isEmpty()) {
			title.addProperty("errorcode", 400);
			return title;
		}
		title.addProperty("title", t.title);
		title.addProperty("ppn", t.ppn);
		title.addProperty("volume", t.volume);
		title.addProperty("author", t.author);
		title.addProperty("co_authors", t.co_authors);
		title.addProperty("edition", t.edition);
		title.addProperty("year_of_creation", t.year_of_creation);
		title.addProperty("classification", t.classification);
		title.addProperty("internal_codes", t.internal_codes);
		title.addProperty("material", t.material);

		JsonArray copies = new JsonArray();
		for (Copy c : t.copies) {
			JsonObject copy = new JsonObject();
			copy.addProperty("signature", c.signature);
			copy.addProperty("location", c.location);

			if (!c.remark_intern.isEmpty() && !c.remark.isEmpty())
				copy.addProperty("remark", c.remark + " | " + c.remark_intern);
			else if (c.remark.isEmpty())
				copy.addProperty("remark", c.remark_intern);
			else if (c.remark_intern.isEmpty())
				copy.addProperty("remark", c.remark);

			copy.addProperty("status", c.status);
			JsonArray stats = new JsonArray();
			for (Statistic s : c.stats) {
				JsonObject stat = new JsonObject();
				stat.addProperty("year", s.year);
				stat.addProperty("num_loans", s.num_loans);
				stats.add(stat);
			}
			copy.add("stats", stats);
			copies.add(copy);
		}
		title.add("copies", copies);
		return title;
	}
}
