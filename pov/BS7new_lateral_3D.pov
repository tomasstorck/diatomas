  #declare Animation  = 0;
  #declare viewmode   = 3;
                      
  #if (Animation)
    #declare FileNumber = str(clock,-4, 0)
    #declare PathInc    = "../demo/output/"
    #declare FullName   = concat(PathInc,"movement",FileNumber,".pov")
  #else
    #declare PathInc    = "../demo/output/"
    #declare FullName   = concat(PathInc,"movement0000.pov")
  #end
  
  // #declare Lx = 400;
  // #declare Lz = 400;      
  // #declare Ly = 100; 
  
  #declare Lx = 1800;
  #declare Lz = 1800;      
  #declare Ly = 1800;
  

  camera {                   
    #if (viewmode=1)
      // view in perspective
      location <-1.25*Lx, 0.8*Ly,-1*Lz> 
      look_at  <Lx/2, Ly/4, Lz/2>
      
    #end
    #if (viewmode=2)
      // view from the top
      location <Lx/2, 2*Lz, Lz/2>   
      look_at  <Lx/2, 0,    Lz/2>                                   
    #end 
    #if (viewmode=3)
      // lateral view
      location <0.5*Lx, Ly/4, -1.3*Lz> 
      look_at  <0.5*Lx, Ly/2, 5*Lz>
    #end

    angle 50
  }
                       
  background { color rgb <1, 1, 1> }
             
  #if (viewmode=1)             
    light_source { < Lx/2,  2*Lz,  Lz/2> color rgb <1,1,1> }
    light_source { < Lx/2,  2*Lz,  Lz/2> color rgb <1,1,1> }
  #end
  #if (viewmode=2)             
    light_source { < Lx/2,  3*Lz,  Lz/2> color rgb <1,1,1> }
    light_source { < Lx/2,  3*Lz,  Lz/2> color rgb <1,1,1> }
  #end
  #if (viewmode=3)             
    light_source { < Lx/2,  2*Ly,  -Lz/2> color rgb <1,1,1> }
    light_source { < Lx/2,  2*Ly,  -Lz/2> color rgb <1,1,1> }
  #end
 
  #declare mtime = str(5*clock*10/1000, -5, 3)  
  #declare gtime = str(floor(5*clock*10/1000/4), 2, 0)                          
  #declare textmessage1 = concat("Movement time : ", mtime, " s") 
  #declare textmessage2 = concat("Growth time : ", gtime, " hours")
                              
  text {           
    ttf "timrom.ttf" textmessage2 0.05, 0.1*x
    pigment {color rgb <1.000, 0.000, 0.000>  }     
        scale <30,30,30> 
        translate <950,800,-1100>  
        rotate 0  
	
  }    
  text {           
    ttf "timrom.ttf" textmessage1 0.05, 0.1*x
    pigment {color rgb <1.000, 0.000, 0.000>  }     
        scale <30,30,30> 
        translate <950,840,-1100>  
        rotate 0  
	
  }
    
                    
  union{     
  
    box {
      <-Lx, -2, -Lz>,  // Near lower left corner
      <2*Lx, 0, 2*Lz>
      texture {
        pigment{
          color rgb <0.15, 0.15, 0.15>
        }
        finish {
          ambient .2
          diffuse .6
        }      
      }
    }
                  
                                  
    #include  FullName                          
     
    translate <   0,  0,  0>         
    rotate    <   0,  0,  0>
  }
