import java.util.*;

public class SemSym {
    private String type;
    private String name;
    private List<String> params;
	private HashMap<String, SemSym> stField;
	private int scopeLevel;
    
    public SemSym(String type) {
        this.type = type;
    }
    
    public SemSym(String type, List<String> params) {
		this.type = type;
		this.params = params;
    }
    
    public SemSym(String type, HashMap<String, SemSym> stField) {
		this.type = type;
		this.stField = stField;
    }
    
    public String getType() {
    	return type;
    }
    
    public String toString() {
    	String rString = new String();
    	rString = name+"("+type+")"+scopeLevel;
        return rString;
    }
    
    public List<String> getFormalsListTypes(){
		return this.params;
	}
	
	public HashMap<String, SemSym> getStField(){
		return stField;
	}
}
