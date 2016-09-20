package util;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Util {	
	public static String simplifyMethodSignature(String signature){
		String simplifiedMethodSignature = "";
		String firstPart = "";
		String parameters= "";
		String lastPart  = "";
		String aux 		 = "";

		//signature = signature.replaceAll("\\s+","");

		for (int i = 0, n = signature.length(); i < n; i++) {
			char chr = signature.charAt(i);
			if (chr == '('){
				aux = signature.substring(i+1,signature.length());
				firstPart+="(";
				break;
			}else
				firstPart += chr;
		}
		for (int i = 0, n = aux.length(); i < n; i++) {
			char chr = aux.charAt(i);
			if (chr == ')'){
				lastPart = aux.substring(i,aux.length());
				break;
			}else
				parameters += chr;
		}

		simplifiedMethodSignature = firstPart + normalizeParameters(parameters) + lastPart;
		simplifiedMethodSignature = simplifiedMethodSignature.replace("{FormalParametersInternal}", "");
		return removeGenerics(simplifiedMethodSignature);
	}
	
	private static String normalizeParameters(String parameters){
		String normalizedParameters = "";
		String[] strs = parameters.split("-");
		for(int i = 0; i < strs.length; i++){
			if(i % 2 == 0){
				normalizedParameters+=(strs[i]+",");
			}
		}
		normalizedParameters = (normalizedParameters.substring(0,normalizedParameters.length()-1)) + "";
		return normalizedParameters;
	}
	
	public static List<String> getArgs(String signature){
		String parameters= "";
		String aux 		 = "";

		signature = signature.replaceAll("\\s+","");

		for (int i = 0, n = signature.length(); i < n; i++) {
			char chr = signature.charAt(i);
			if (chr == '('){
				aux = signature.substring(i+1,signature.length());
				break;
			}
		}
		for (int i = 0, n = aux.length(); i < n; i++) {
			char chr = aux.charAt(i);
			if (chr == ')'){
				break;
			}else
				parameters += chr;
		}
		return (parameters.equals("") ? new ArrayList<String>()  : new ArrayList<String>(Arrays.asList(parameters.replace("{FormalParametersInternal}", "").split(","))));		
	}
		
	private static boolean isStatic(String str) {
		return str.equalsIgnoreCase("static");
	}

	private static boolean isAccessModifier(String str) {
		return str.equalsIgnoreCase("private")
				|| str.equalsIgnoreCase("public")
				|| str.equalsIgnoreCase("protected");
	}
	
	private static boolean isMethodModifier(String str)
	{
		return Util.isAccessModifier(str)
				|| isStatic(str)
				|| str.equalsIgnoreCase("abstract")
				|| str.equalsIgnoreCase("final")
				|| str.equalsIgnoreCase("native")
				|| str.equalsIgnoreCase("strictfp")
				|| str.equalsIgnoreCase("synchronized");
	}
	
	public static boolean isStaticMethod(List<String> modifiersList)
	{
		return modifiersList.contains("static");
	}
	
	public static boolean isPrivateMethod(List<String> modifiersList)
	{
		return modifiersList.contains("private");
	}
	
	private static List<String> getModifiersList(String str)
	{
		String[] strs = str.split("\\s+");
		List<String> modifiers = new ArrayList<String>();
		int i = 0;
		while(i < strs.length && isMethodModifier(strs[i]))
		{
			modifiers.add(strs[i]);
			i++;
		}
		return modifiers;
	}
		
	private static boolean isPrivate(String str)
	{
		return str.equalsIgnoreCase("private");
	}
	
	private static boolean isPublic(String str)
	{
		return str.equalsIgnoreCase("public");
	}
	
	private static String removeGenerics(String methodSignature) {
		String res = "";
		int count = 0;
		boolean canTake = true;
		for (int i = 0, n = methodSignature.length(); i < n; i++) {
			char chr = methodSignature.charAt(i);
			if (chr == '<') {
				if (canTake) {
					canTake = false;
					count = 1;
				} else {
					count++;
				}
			} else if (chr == '>') {
				count--;
				canTake = count == 0;
			}else if(canTake)
			{
				res += chr;
			}
		}
		return res;
	}
	
	public static String getFullType(String arg, List<String> imports, String packageName, String packagePath)
	{
		String fullType = arg;
		if(!isPrimitiveType(arg))
		{
			String[] importSplit;
			String importStr = "";
			int j = 0;
			boolean found = false;
			String typeName = arg.replace("[]", "");
			while(j < imports.size() && !found)
			{
				importStr = imports.get(j);
				importSplit = importStr.split("\\.");
				
				found = importSplit[importSplit.length - 1].equals(typeName);

				j++;
			}
			if(!found)
			{
				File packageFolder = new File(packagePath);
				String fileName;
				int k = 0;
				while(packageFolder.listFiles() != null && k < packageFolder.listFiles().length && !found)
				{
					File file = packageFolder.listFiles()[k];
					fileName = file.getName().replace(".java", "");
					found = file.isFile() && file.getName().endsWith(".java") && typeName.equals(fileName);
					if(found)
					{
						if(!packageName.equals("(default package)"))
						{
							importStr = packageName + "." + typeName;
						}
						
					}
					k++;
				}
			}
			if(found)
			{
				fullType = arg.replace(typeName, importStr);
			}
		}
		return fullType;
	}
	
	public static String includeFullArgsTypes(String signature, String body, List<String> imports, String packageName, String packagePath)
	{
		/*String[] splittedSignature = signature.split("\\(");
		String signatureMethName = splittedSignature[0];
		String[] splittedBody = body.split(signatureMethName);
		String fullBodyAfterName = splittedBody[1];
		String bodyAfterName = fullBodyAfterName.split("\\(")[1];
		String[] splittedBodyAfterName = bodyAfterName.split("\\)");
		String bodyParamsPart = splittedBodyAfterName[0];
		*/
		String firstPatt = "((\\s)*(final))?(\\s)*([^\\.\\s]+[\\.[^\\.\\s]+]*)";
		String sep = "(\\s)*(\\.\\.\\.)(\\s)*";
		String secondPatt = "[^\\.\\s]+(\\s)*";
		String patt = firstPatt + sep + secondPatt;
		String internalPatt = firstPatt + "(" + sep + "|(\\s)+)" + secondPatt;
		Pattern MY_PATTERN = Pattern.compile("\\(("+internalPatt+")?(,"+internalPatt+")*\\)");
		Matcher m = MY_PATTERN.matcher(body);
		String bodyParamsPart = "";
		if(m.find()) {
		    //System.out.println("matched");
			String s = m.group();
		    //System.out.println(s.substring(1, s.length() - 1));
		    bodyParamsPart = s.substring(1, s.length() - 1);
		}
		//System.out.println("BODYPARAMSPART: "+bodyParamsPart);
		String[] bodyParams = bodyParamsPart.split(",");
		List<String> args = getArgs(signature);
		String oldArgsStr = String.join(",", args);
		int i = 0;
		for(String arg : args)
		{
			String fullType = getFullType(arg, imports, packageName, packagePath);
			if(!fullType.equals(arg))
			{
				args.set(i, fullType);
			}
			if(!arg.trim().endsWith("[]") && bodyParams.length > i &&
					((bodyParams[i].trim().endsWith("[]")) || (bodyParams[i].matches(patt))))
			{				
				args.set(i, args.get(i) + "[]");
			}
			i++;
		}
		String newArgsStr = String.join(",", args);
		int paramsBegin = signature.indexOf("(");
		String methodName = signature.substring(0, paramsBegin);
		String paramsPart = signature.substring(paramsBegin);
		return methodName + paramsPart.replace(oldArgsStr, newArgsStr);
	}
		
	public static boolean isPrimitiveType(String typeStr)
	{
		return typeStr.equals("byte") ||
				typeStr.equals("short") ||
				typeStr.equals("int") ||
				typeStr.equals("long") ||
				typeStr.equals("float") ||
				typeStr.equals("double") ||
				typeStr.equals("char") ||
				typeStr.equals("boolean");
	}
	
	private static boolean isGeneric(String str) {
		return str.startsWith("<");
	}
		
	public static String getMethodReturnType(String methodSignature, List<String> imports, String packageName, String packagePath) {
		String filtMethodSign = methodSignature.replaceAll("// LEFT //", "").replaceAll("// RIGHT //", "");
		String partialFiltered = filtMethodSign.replaceAll("/\\*.*\\*/", "");
		String simplMethodSignature = partialFiltered.replaceAll("//.*\n","\n");
		String[] strs = simplMethodSignature.split("\\s+");
		int i = 0;
		while(i < strs.length && strs[i].startsWith("@"))
		{
			i++;
			if(i < strs.length && strs[i - 1].contains("(") && !strs[i - 1].contains(")"))
			{				
				while(i < strs.length && !strs[i].contains(")")){
					i++;
				}
				i++;
			}
		}
		while(i < strs.length && isMethodModifier(strs[i]))
		{
			i++;
		}
		String returnType = "";
		if(i < strs.length)
		{
			if (!isGeneric(strs[i])) {
				returnType = strs[i];
			} else {
				returnType = getMethodReturnType(removeGenerics(methodSignature), imports, packageName, packagePath);
			}
		}		
		returnType = removeGenerics(returnType);
		return returnType.equals("void") ? returnType : getFullType(returnType, imports, packageName, packagePath);
	}
		
	public static void main(String[] args) {
		System.out.println(getArgs("soma(int-int-boolean-boolean) throws Exception"));
		System.out.println(getArgs("int soma()"));
		System.out.println(getArgs("public static void int soma(List<Integer>-List<Integer>-int-int)"));
		System.out.println(simplifyMethodSignature("soma(List<Integer>-List<Integer>-int-int) throws Exeception"));
		System.out.println(simplifyMethodSignature("soma()"));
		System.out.println(getArgs(simplifyMethodSignature("soma(List<Integer>-List<Integer>-int-int) throws Exeception")));
		System.out.println(removeGenerics("public void soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}"));
		String str = "<T, S extends T> int copy(List<T> dest, List<S> src) {";
		System.out.println(removeGenerics("soma(List<Integer>-List<Integer>-int-int)"));
		System.out.println(removeGenerics(simplifyMethodSignature("soma(List<Integer>-List<Integer>-int-int) throws Exeception")));
		
		List<String> imports = new ArrayList<String>();
		imports.add("rx.Scheduler");
		imports.add("cin.ufpe.br.A");
		imports.add("java.util.List");
		System.out.println("HERE");
		System.out.println(includeFullArgsTypes(removeGenerics(simplifyMethodSignature(("soma(List<Integer>-List<Integer>-int-int) throws Exeception"))),"public int soma(List<Integer> a, int b) {}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(includeFullArgsTypes(removeGenerics(simplifyMethodSignature(("soma(List<Integer>-List<Integer>-Scheduler-Scheduler) throws Exeception"))), "public int soma(List<Integer> a, Scheduler b) {}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(includeFullArgsTypes(removeGenerics(simplifyMethodSignature(("soma(String-String-Scheduler-Scheduler-Object-Object) throws Exeception"))), "public int soma(String a, Scheduler b, Object c) {}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(includeFullArgsTypes(removeGenerics(simplifyMethodSignature(("soma(String[]-String[]-Scheduler[]-Scheduler[]-Object-Object) throws Exeception"))), "public int soma(String[] a, Scheduler[] b, Object c) {}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(includeFullArgsTypes(removeGenerics(simplifyMethodSignature(("soma(Character.Subset-Character.Subset) throws Exeception"))), "public int soma(Character.Subset a[]) {}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(removeGenerics("public List<Integer> soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}"));
		String aux = "@Command(\n"+
"            aliases = { \"load\", \"l\" },\n" +
"            usage = \"[format] <filename>\",\n" +
"// LEFT //            desc = \"Load a file into your clipboard\",\n" +
"// LEFT //            help = \"Load a schematic file into your clipboard\n\" + \n" +
"                    \"Format is a format from \"//schematic formats\"\n \" +\n" +
"                    \"If the format is not provided, WorldEdit will\n\" +\n" +
"                    \"attempt to automatically detect the format of the schematic\", \n" +
"            flags = \"f\",\n"+
"            min = 1,\n"+
"            max = 2\n" +
"    )\n"+
"    @CommandPermissions({\"worldedit.clipboard.load\", \"worldedit.schematic.load\"}) /* TODO: Remove 'clipboard' perm*/\n"+
"// RIGHT //    public void load(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException, CommandException {\n"+
"// RIGHT //        // TODO: Update for new clipboard\n"+
"// RIGHT //        throw new CommandException(\"Needs to be re-written again\");\n"+
"    }";
		System.out.println(getMethodReturnType(aux, imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		aux = "@Command(\n"+
				"            aliases = { \"load\", \"l\" },\n" +
				"            usage = \"[format] <filename>\",\n" +
				"// LEFT //            desc = \"Load a file into your clipboard\",\n" +
				"// LEFT //            help = \"Load a schematic file into your clipboard\n\" + \n" +
				"                    \"Format is a format from \"//schematic formats\"\n \" +\n" +
				"                    \"If the format is not provided, WorldEdit will\n\" +\n" +
				"                    \"attempt to automatically detect the format of the schematic\", \n" +
				"            flags = \"f\",\n"+
				"            min = 1,\n"+
				"            max = 2\n" +
				"    )\n"+
				"    @CommandPermissions({\"worldedit.clipboard.load\", \"worldedit.schematic.load\"}) // TODO: Remove 'clipboard' perm\n"+
				"// RIGHT //    public void load(Player player, LocalSession session, EditSession editSession, CommandContext args) throws WorldEditException, CommandException {\n"+
				"// RIGHT //        // TODO: Update for new clipboard\n"+
				"// RIGHT //        throw new CommandException(\"Needs to be re-written again\");\n"+
				"    }";
		System.out.println(getMethodReturnType(aux, imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public List<Integer> soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public static List<Integer> soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public void soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public static void soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public int soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public static int soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public static synchronized int soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public static native synchronized int soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("static synchronized int soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("synchronized int soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println("Type: "+getMethodReturnType("@Override   "
				+ "synchronized int soma(List<Integer> a, List<Integer> b, int c, int d) throws Exeception {return 1;}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public Hello teste(JoanaEntryPoint a, List<Integer> b, Object c){}", imports, "(default package)", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src"));
		System.out.println(getMethodReturnType("public B teste(JoanaEntryPoint a, List<Integer> b, Object c){}", imports, "paramsEx", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src/paramsEx"));
		System.out.println(getMethodReturnType("@Test(timeout = 10000)"
    + "    public void testIssue2890NoStackoverflow() throws InterruptedException {"
       + "assertEquals(n, counter.get());"
    + "}", imports, "(default package)",""));
		System.out.println(getMethodReturnType("@Test(timeout=10000)"
			    + "    public void testIssue2890NoStackoverflow() throws InterruptedException {"
			       + "assertEquals(n, counter.get());"
			    + "}", imports, "(default package)",""));
			    
		System.out.println(includeFullArgsTypes(simplifyMethodSignature("main(String-String-String[]-String[]-String-String)"), "public static void main(String args[], String[] args2, String args3) {int a;}", imports, "", ""));
		System.out.println(includeFullArgsTypes(simplifyMethodSignature("main(String[]-String[])"),"public static void main(String[] args) {int a;}", imports, "", ""));
		System.out.println("OUT: "+includeFullArgsTypes(simplifyMethodSignature("main(String-String)"),"public static void main(String ... args) {int a;}", imports, "", ""));
		System.out.println(includeFullArgsTypes(simplifyMethodSignature("main(String-String)"),"public static void main(String  ...   args) {int a;}", imports, "", ""));
		System.out.println(includeFullArgsTypes(simplifyMethodSignature("main(String-String-String[]-String[]-String-String-String-String-String-String-String-String)"), "public static void main(String args[], String[] args2, String ...args5, String args3, String...args4, String... args6) {int a;}", imports, "", ""));
		System.out.println(includeFullArgsTypes(simplifyMethodSignature("main(String-String-String[]-String[]-String-String-String-String-String-String-String-String)"), "public static void main(String args[], String[] args2, String ...args5, String args3, String...args4, String ... args6) {int a;}", imports, "", ""));
		System.out.println(includeFullArgsTypes(simplifyMethodSignature("main(String-String)"),"public static void main(final String  ...   args) {int a;}", imports, "", ""));
		System.out.println(includeFullArgsTypes(simplifyMethodSignature("main(String-String-String[]-String[]-String-String-String-String-String-String-String-String)"), "public static void main(String args[], final String[] args2, String ...args5, final String args3, String...args4, String... args6) {int a;}", imports, "", ""));
		System.out.println(includeFullArgsTypes(simplifyMethodSignature("main(String-String-String[]-String[]-String-String-String-String-String-String-String-String)"), "public static void main(String args[],final String[] args2, String ...args5, String args3, String...args4, String ... args6) {int a;}", imports, "", ""));
		//System.out.println(includeFullArgsTypes(removeGenerics(simplifyMethodSignature(("soma(B[]-B[]-C-C-Object-Object-Hello-Hello) throws Exeception"))), imports, "paramsEx", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src/paramsEx"));
		//System.out.println(includeFullArgsTypes("longAndAdd()", imports, "rx.internal.util","/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/downloads/RxJava/revisions/rev_5d513_a9cd9/rev_5d513-a9cd9/src/test/java/rx/internal/util"));
		//System.out.println(includeFullArgsTypes("void redis.clients.jedis.BinaryJedis.initializeClientFromURI(URI)", new ArrayList<String>(Arrays.asList(new String[]{"java.net.URI"})),"paramsEx", "/Users/Roberto/Documents/UFPE/Msc/Projeto/conflicts_analyzer/TestFlows/src/paramsEx"));*/
	}
}