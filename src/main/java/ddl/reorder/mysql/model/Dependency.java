/**
 * 
 */
package ddl.reorder.mysql.model;

import lombok.Data;

/**
 * @author vadherak
 *
 */
@Data
public class Dependency implements Comparable<Dependency> {

	private String type;
	private String keyName;
	private String column;
	private String depOnTableName;
	private String depOnTableColumn;
	private Table depOnTable;
	private Table srcTable;
	private boolean isSatisfied = false;

	

	@Override
	public int compareTo(Dependency o) {
		return this.getKeyName().compareTo(o.getKeyName());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Dependency) {
			return this.getKeyName().equals(((Dependency) obj).getKeyName());
		}
		return false;
	}

}
