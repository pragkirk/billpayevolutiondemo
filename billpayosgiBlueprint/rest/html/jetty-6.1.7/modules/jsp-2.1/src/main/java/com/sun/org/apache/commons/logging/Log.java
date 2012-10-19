// ========================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at 
// http://www.apache.org/licenses/LICENSE-2.0
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ========================================================================
package com.sun.org.apache.commons.logging;





public interface Log 
{
    public  void fatal (Object message);
    
    
    public  void fatal (Object message, Throwable t);
   
    
    public  void debug(Object message);
   
    
    public  void debug (Object message, Throwable t);
   
    
    public  void trace (Object message);
   
    
  
    public  void info(Object message);
   

    public  void error(Object message);
   
    
    public  void error(Object message, Throwable cause);
   

    public  void warn(Object message);
  
    
    public  boolean isDebugEnabled ();
    
    
    public  boolean isWarnEnabled ();
    
    
    public  boolean isInfoEnabled ();
    
    
    
    public  boolean isErrorEnabled ();
   
    
  
    public  boolean isTraceEnabled ();
   
}
