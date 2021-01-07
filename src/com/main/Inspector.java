


package com.main;
import java.util.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Inspector {



    private Map<String,ArrayList<String>> values = new HashMap<>();
    private  Map<String, String> common_regex_patterns =  new HashMap<>();
    private  Map<String, String> special_regex_patterns =  new HashMap<>();
    private ArrayList<String> signatures = new ArrayList<>();

    //By country requiremetns
    private ArrayList<String> foreigners_requirements = new ArrayList<>();
    private ArrayList<String> citizens_requirements = new ArrayList<>();
    private ArrayList<String> entrants_requirements = new ArrayList<>();
    private ArrayList<String> allowed_nations  = new ArrayList<>();

    //other requirements
    private Map<String,ArrayList<String>> required_vaccinations  = new HashMap<>();
    private ArrayList<String> workers_requirements = new ArrayList<>();
    private String wanted_by_the_state="";

    //other utility objects
    final private LocalDate expirienceDate = LocalDate.of(1982,11,22);
    private Matcher matcher;
    private Pattern pattern;
    private String tempForBulletin;

    public Inspector()
    {

        common_regex_patterns.put("DURATION","DURATION: (\\d+ [A-Z]+|[A-Z]+)");
        common_regex_patterns.put("PURPOSE","PURPOSE: ([A-Z]+)");
        common_regex_patterns.put("WEIGHT","WEIGHT: (\\d{2,3})");
        common_regex_patterns.put("HEIGHT","HEIGHT: (\\d{2,3})");
        common_regex_patterns.put("nationality","NATION: ([A-Z][a-z]+\\s*[A-Z]*[a-z]*)\n+");
        common_regex_patterns.put("ISS","ISS: ([A-Z][a-z]+.?\\s*[A-Z]*[a-z]*)\n+");
        common_regex_patterns.put("DOB","DOB: (\\d{4}\\.+\\d{2}\\.+\\d{2})");
        common_regex_patterns.put("SEX","SEX: (F|M)");
        common_regex_patterns.put("ID number","ID#: (\\w*-*\\w*)");
        common_regex_patterns.put("FIELD","FIELD: ([A-Z][a-z]+\\s*[a-z]*)");


        special_regex_patterns.put("name","NAME: ([A-Z]\\w+),+\\s([A-Z][a-z]+)");
        special_regex_patterns.put("EXP","EXP: (\\d{4}\\.+\\d{2}\\.+\\d{2})");

        signatures.addAll(common_regex_patterns.keySet());
        signatures.addAll(special_regex_patterns.keySet());
        signatures.add("DOCUMENT");
        signatures.add("ACCESS");
        signatures.add("VACCINESS");

    }

    public void receiveBulletin(String bulletin) {

        String[] req = bulletin.split("\n");

        for (String s : req) {

            if (s.matches("Allow citizens of.+")) {
                pattern = Pattern.compile("(?:Allow citizens of)? ([A-Z][a-z]+\\s*[A-Z]*[a-z]*),*");
                matcher = pattern.matcher(s);
                while (matcher.find()) {
                    allowed_nations.add(matcher.group(1));
                }
            } else if (s.matches("Deny citizens of.+")) {

                pattern = Pattern.compile("(?: Deny citizens of)? ([A-Z][a-z]+\\s*[A-Z]*[a-z]*),*");
                matcher = pattern.matcher(s);
                while (matcher.find()) {
                    allowed_nations.remove(matcher.group(1));
                }
            } else if (s.matches("\\D+ no longer require (\\w+\\s?\\w*) vaccination")) {

                pattern = Pattern.compile("\\D+ no longer require (\\w+\\s?\\w*) vaccination");
                matcher = pattern.matcher(s);
                matcher.find();

                String vaccination = matcher.group(1);
                ArrayList<String> nations = new ArrayList<>();

                pattern  = Pattern.compile("(?:^Citizens of|(?!^)\\G,) ([A-Z][a-z]+(?: [A-Z][a-z]+)*)(?=[a-zA-Z, ]*?)");
                matcher = pattern.matcher(s);
                while (matcher.find())
                    nations.add(matcher.group(1));

                if(!nations.isEmpty()) {
                    while (!nations.isEmpty()) {
                        required_vaccinations.get(vaccination).remove(nations.remove(0));
                    }
                }
                else if (s.contains("Foreigners"))
                    required_vaccinations.get(vaccination).remove("FOREIGNERS");
                else
                    required_vaccinations.get(vaccination).remove("ENTRANTS");


            } else if (s.matches("\\D+ require (\\w+\\s?\\w*) vaccination")) {

                //thanks to: https://stackoverflow.com/users/548225/anubhava for commitment in this regex pattern
                pattern  = Pattern.compile("(?:^Citizens of|(?!^)\\G,) ([A-Z][a-z]+(?: [A-Z][a-z]+)*)(?=[a-zA-Z, ]*?)");
                matcher = pattern.matcher(s);

                ArrayList<String> nations = new ArrayList<>();
                while (matcher.find())
                    nations.add(matcher.group(1));

                pattern = Pattern.compile("\\D+ require (\\w+\\s?\\w*) vaccination");
                matcher = pattern.matcher(s);
                matcher.find();
                if(!nations.isEmpty())
                    required_vaccinations.put(matcher.group(1), nations);
                else if (s.contains("Foreigners"))
                {
                    nations.add("FOREIGNERS");
                    required_vaccinations.put(matcher.group(1),nations);
                }
                else
                {
                    nations.add("ENTRANTS");
                    required_vaccinations.put(matcher.group(1),nations);

                }

            } else if (s.matches("Foreigners require \\D+")) {
                change(s, "(?:Foreigners require) (\\D+)", foreigners_requirements);
            } else if (s.matches("Workers require \\D+")) {
                change(s, "(?:Workers require) (\\D+)", workers_requirements);
            } else if (s.matches("Citizens of Arstotzka require \\D+")) {
                change(s, "(?:Citizens of Arstotzka require) (\\D+)", citizens_requirements);
            } else if (s.matches("Wanted by the State: \\D+")) {
                Pattern pattern = Pattern.compile(("(?:Wanted by the State: )(\\D+)"));
                Matcher matcher = pattern.matcher(s);
                matcher.find();
                wanted_by_the_state = matcher.group(1);
            } else if (s.matches("Entrants require \\D+")) {
                change(s, "(?:Entrants require) (\\D+)", entrants_requirements);
            }
        }
    }
    private void change(String word, String regex, ArrayList<String> col)
    {
        pattern = Pattern.compile(regex);
        matcher = pattern.matcher(word);
        matcher.find();
        col.add(matcher.group(1));
    }


    private String check(ArrayList<String> v, String type)
    {
        if (type == "DOCUMENT" || type == "EXP" || type == "ACCESS" || type == "VACCINESS")
            return "";
        if(v.stream().distinct().count() > 1)
            return "Detainment: " + type + " mismatch.";
        return "";
    }


    private String vaccineCheck()
    {
        ArrayList<String> vacineSet = new ArrayList<>();
        vacineSet.addAll(required_vaccinations.keySet());

        for (int i =0; i < vacineSet.size(); i++) {
            ArrayList<String> nations = required_vaccinations.get(vacineSet.get(i));
            for(String nation : nations) {

                if ((nation.equals("FOREIGNERS") && !values.get("nationality").get(0).equals("Arstotzka")) || nation.equals("ENTRANTS") || nation.equals(values.get("nationality").get(0))) {
                    if (!values.get("DOCUMENT").contains("certificate of vaccination"))
                        return "Entry denied: missing required certificate of vaccination.";
                    if (values.get("VACCINESS").get(0).contains(vacineSet.get(i)))
                        continue;
                    return "Entry denied: missing required vaccination.";
                }
            }
        }
        return "";
    }

    private String expChecking(String date)
    {
        if(date == null)
            return "";

        LocalDate localDate = LocalDate.parse(date.substring(0,10), DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        if(localDate.isAfter(expirienceDate) || localDate.isEqual(expirienceDate))
            return "";
        return  "Entry denied: " + date.substring(11) + " expired.";

    }


    public String inspect(Map<String,String> a )
    {
        resetValues();


        for(String ks : a.keySet())
            this.setDocument(ks,a.get(ks));

        if (values.get("name").contains(wanted_by_the_state))
            return "Detainment: Entrant is a wanted criminal.";
        for(int i = 0; i < values.size(); i++)
        {
            String test = check(values.get(signatures.get(i)),signatures.get(i));
            if(!test.isEmpty())
                return test;
        }


        for(int i = 0 ; i < values.get("EXP").size(); i++)
        {
            String test =  expChecking(values.get("EXP").get(i));
            if(test.equals(""))
                continue;
            else return test;
        }
        for(int i = 0; i < entrants_requirements.size(); i++)
        {
            if (!values.get("DOCUMENT").contains(entrants_requirements.get(i)))
            {
                return "Entry denied: missing required " + entrants_requirements.get(i) + ".";
            }
        }

        if (!values.get("nationality").get(0).equals("Arstotzka"))
        {
            for(int i = 0; i < foreigners_requirements.size(); i++)
                if (!values.get("DOCUMENT").contains(foreigners_requirements.get(i))){
                    if(foreigners_requirements.get(i).equals("access permit"))
                    {
                        if(values.get("DOCUMENT").contains("diplomatic authorization"))
                        {
                            if(!values.get("ACCESS").get(0).contains("Arstotzka"))
                                return "Entry denied: invalid diplomatic authorization.";
                            else
                                continue;
                        }
                        else if (values.get("DOCUMENT").contains("grant of asylum"))
                            continue;
                        else
                            return "Entry denied: missing required " + foreigners_requirements.get(i) + ".";
                    }
                    return "Entry denied: missing required " + foreigners_requirements.get(i) + ".";
                }
        }
        if(!allowed_nations.contains(values.get("nationality").get(0)))
            return "Entry denied: citizen of banned nation.";

        if(values.get("PURPOSE").contains("WORK") && workers_requirements.contains("work pass"))
        {
            if(!values.get("DOCUMENT").contains("work pass"))
                return "Entry denied: missing required work pass.";
        }

        String vaccin =  vaccineCheck();
        if(!vaccin.isEmpty())
            return vaccin;



        if(values.get("nationality").get(0).equals("Arstotzka") && citizens_requirements.contains("ID card") && !values.get("DOCUMENT").contains("ID card"))
            return "Entry denied: missing required ID card.";

        if(values.get("nationality").get(0).equals("Arstotzka")) {
            return "Glory to Arstotzka.";
        }


        return "Cause no trouble.";


    }

    private void setDocument(String docName, String document)
    {
        String doc = docName.replace("_"," ");
        values.get("DOCUMENT").add(doc);
        getName(document);
        getDate(doc,document);
        if(docName.equals("diplomatic_authorization"))
        {

            values.get("ACCESS").add(document);

        }
        if(docName.equals("certificate_of_vaccination"))
        {
            values.get("VACCINESS").add(document);
        }
        for (String patterns : common_regex_patterns.keySet())
        {
            pattern = Pattern.compile(common_regex_patterns.get(patterns));
            matcher = pattern.matcher(document);
            if(matcher.find())
                values.get(patterns).add(matcher.group(1));
        }

    }
    private void getDate(String docName, String document)
    {
        pattern = Pattern.compile(special_regex_patterns.get("EXP"));
        matcher = pattern.matcher(document);
        if(matcher.find())
            values.get("EXP").add(matcher.group(1) + " " + docName);
    }
    private void getName(String name)
    {
        pattern = Pattern.compile(special_regex_patterns.get("name"));
        matcher = pattern.matcher(name);
        if(matcher.find());
        values.get("name").add(matcher.group(2)+" "+matcher.group(1));
    }

    private void resetValues()
    {
        values = new HashMap<>();
        for (String s : signatures) values.put(s, new ArrayList<>());
    }
}