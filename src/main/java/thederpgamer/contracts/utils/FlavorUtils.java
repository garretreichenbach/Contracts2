package thederpgamer.contracts.utils;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class FlavorUtils {

	/*
	private static String getBaseForm(String word) {
		Document doc = new Document(word);
		Sentence sent = doc.sentences().get(0);
		List<String> lemmas = sent.lemmas();
		if(!lemmas.isEmpty()) return lemmas.get(0);
		return word;
	}
	 */

	public static String generateSpawnName(FlavorType flavorType) {
		String adjectiveName = flavorType.adjectiveNames[(int) (Math.random() * flavorType.adjectiveNames.length)];
		String name = flavorType.names[(int) (Math.random() * flavorType.names.length)];
//		do name = flavorType.names[(int) (Math.random() * flavorType.names.length)];
//		while(getBaseForm(adjectiveName).equalsIgnoreCase(getBaseForm(name)));
		return flavorType.prefix + "-" + adjectiveName + " " + name;
	}

	public static String generateGroupName(FlavorType flavorType) {
		String adjectiveName = flavorType.adjectiveNames[(int) (Math.random() * flavorType.adjectiveNames.length)];
		String name = flavorType.names[(int) (Math.random() * flavorType.names.length)];
//		do name = flavorType.names[(int) (Math.random() * flavorType.names.length)];
//		while(getBaseForm(adjectiveName).equalsIgnoreCase(getBaseForm(name)));
		return "The " + adjectiveName + " " + name + "s";
	}

	public enum FlavorType {
		TRADERS("TGS", new String[] {
				"Profitable", "Mercantile", "Trading", "Commercial", "Business", "Economic", "Financial", "Monetary", "Capital", "Market", "Retail", "Wholesale", "Distribution", "Profiteering"
		}, new String[]{
				"Trader", "Merchant", "Dealer", "Broker", "Peddler", "Hawker", "Salesman", "Supplier", "Wholesaler", "Retailer", "Distributor", "Profiteer", "Vendor", "Marketer", "Salesman"
		}),
		MERCENARIES("MCS", new String[] {

		}, new String[]{

		}),
		PIRATE("PRS", new String[] {
				"Crimson", "Hardy", "Marauding", "Raiding", "Ruthless", "Savage", "Scourge", "Vicious", "Black", "Bloody", "Cutthroat", "Dread", "Fearsome", "Fierce", "Grim", "Merciless", "Red", "Savage", "Sinister", "Vile", "Wicked", "Sly"
		}, new String[]{
				"Pirate", "Buccaneer", "Corsair", "Freebooter", "Privateer", "Raider", "Reaver", "Reaper", "Dog", "Swashbuckler", "Hook", "Hand", "Cannon"
		});

		public final String prefix;
		public final String[] adjectiveNames;
		public final String[] names;

		FlavorType(String prefix, String[] adjectiveNames, String[] names) {
			this.prefix = prefix;
			this.adjectiveNames = adjectiveNames;
			this.names = names;
		}
	}
}
