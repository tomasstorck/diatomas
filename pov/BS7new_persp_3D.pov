  #declare Animation  = 1;
  #declare viewmode   = 1;
                      
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
      location <-1*Lx, 0.9*Ly,-0.7*Lz> 
      look_at  <Lx/2, Ly/4, Lz/2>
      
    #end
    #if (viewmode=2)
      // view from the top
      location <Lx/2, Ly, -Lz>   
      look_at  <Lx/2, 0,    -Lz>                                   
    #end 
    #if (viewmode=3)
      // lateral view
      location <0, -Ly/4, 2*Lz> 
      look_at  <0, -Ly/2, -Lz>
    #end

    angle 50
  }
                       
  background { color rgb <1, 1, 1> }
             
  #if (viewmode=1)             
    light_source { < Lx/2,  10*Ly,  Lz/2> color rgb <1,1,1> }
    light_source { < Lx/2,  10*Ly,  Lz/2> color rgb <1,1,1> }
    light_source { < Lx/2,  10*Ly,  Lz/2> color rgb <1,1,1> }
  #end
  #if (viewmode=2)             
    light_source { < Lx/2,  3*Lz,  Lz/2> color rgb <1,1,1> }
    light_source { < Lx/2,  3*Lz,  Lz/2> color rgb <1,1,1> }
  #end
  #if (viewmode=3)             
    light_source { < 320,  250,  200> color rgb <1,1,1> }
    light_source { < 320,  250,  200> color rgb <1,1,1> }
  #end
 
  #declare mtime = str(5*clock*10/1000, -5, 3)  
  #declare gtime = str(floor(5*clock*10/1000/4), 2, 0)                          
  #declare textmessage1 = concat("Movement time : ", mtime, " s") 
  #declare textmessage2 = concat("Growth time : ", gtime, " hours")
                              
  text {           
    ttf "timrom.ttf" textmessage2 0.05, 0.1*x
    pigment {color rgb <0.000, 0.000, 0.000>  }     
        scale <60,60,60> 
        translate <-1000,1500,-75>  
        rotate <0,45,0>  
	
  }    
  text {           
    ttf "timrom.ttf" textmessage1 0.05, 0.1*x
    pigment {color rgb <0.000, 0.000, 0.000>  }     
        scale <60,60,60> 
        translate <-1000,1580,-75>  
        rotate <0,45,0>    
	
  }
    
                    
  union{     
  
    box {
      <-Lx, -2, -Lz>,  // Near lower left corner
      <3*Lx, 0, 3*Lz>
      texture {
        pigment{
          color rgb <0.1, 0.1, 0.1>
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
