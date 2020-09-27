package com.webmail;

import javax.mail.Address;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Classification {
	
	static String social_Keyword[]={"Friend Request","Facebook","Likes","Accepted your Request","Social","Dating Service","Added you in circle","Updated Profile Picture","Added New Photo","Added","Twitted","Google+","Instagram","Hangouts","Tweet","Popular","LinkedIn","like","Twitter"};
	static String promotion_Keyword[]={"Buy","Join Us","Follow Us","Sign Up","Offers","Connect with us","Get Discount","Deal","Grab Deal","Below","Ebay","eBay","eBay:","eBay!","Starting at","Shopping","Cash on Delivery","Off","Return Policy","Amazon","Ebay","Flipkart","Alibaba","Ebay","Freecharge"};
	
	        
    
    
    static public String getClass(String msg,String email){
    	int p=0,s=0;
        
    	int a=(social_Keyword.length<promotion_Keyword.length)?promotion_Keyword.length:social_Keyword.length;
    	for(int i=0;i<a;i++){
    		
    		if(i<social_Keyword.length){
                    
    			Pattern pat=Pattern.compile(social_Keyword[i]);
                        Matcher mat=pat.matcher(msg);
                        
                       while(mat.find()){
                           
    			if("facebook twitter google+ instagram hangout whatsapp hike linkedin".contains(social_Keyword[i].toLowerCase()))
                            s=s+3;
                          else
                            s++;
    			}
    			}
    		
    		if(i<promotion_Keyword.length){
                    
    			Pattern pat=Pattern.compile(promotion_Keyword[i]);
                        Matcher mat=pat.matcher(msg);
                      
                        while(mat.find()){
    			 if("amazon ebay Ebay flipkart alibaba snapdeal yepme myntra walmart freecharge".contains(promotion_Keyword[i].toLowerCase()))
                            p=p+3;	
                          else  
                            p++;
                         if("ebay alibaba".contains(promotion_Keyword[i].toLowerCase()))
                             p+=8;
    			}
    			
    			}
    			   		
    	}    	
    		
    	if(p<2 && s<2){
    			return "Primary";
    		}
    		
    	if(p<s){
    			return "Social";
    		}
    	else if(s<p){
    			return "Promotion";
    		}
    	else{
    			return "Primary";
    		}
    	
    }
    
   
}