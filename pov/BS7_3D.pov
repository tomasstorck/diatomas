  #declare Animation  = 0;
  #declare viewmode   = 1;       
  #declare cam_z = 3; //the amount of camera zoom you want

  
                      
  #if (Animation)
    #declare FileNumber = str(clock,-6, 0)
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
      location <-Lx*1.3, 1*Ly/.8,  Lz/1>   
      look_at  <Lx/2, -Ly/2.6,   -Lz/8>    

      //location <0, Lz,  -1.8*Lz> 
      //look_at  <Lx/2, 1.5*Ly,    Lz/2>   
      
      //location <-1.5*Lx, Ly, -1*Lz>    
      //location <Lx/2, Ly/4, -Lz/10>
 //     location <-0.5*Lx, 0.4*Ly,-0.5*Lz> 
  //    look_at  <Lx/2, Ly/4, Lz/2>
      
//      location <-1.25*Lx, 1.1*Ly, -1*Lz> 
//      look_at  <5*Lx/6, 1*Ly/6, -0.7*Lz> 

      //location <-Lx, 0.5*Ly, -Lz> 
      //look_at  <Lx/2, Ly/2, Lz/2>
      
       
    #end
    #if (viewmode=2)
      // view from the top
      location <Lx/2, 2*Lz, Lz/2>   
      look_at  <Lx/2, 0,    Lz/2>                                   
    #end 
    #if (viewmode=3)
      // lateral view
      location <Lx/3, Ly/3, 2*Lz> 
      look_at  <0, -Ly/2, -Lz>
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
    light_source { < 320,  250,  200> color rgb <1,1,1> }
    light_source { < 320,  250,  200> color rgb <1,1,1> }
  #end
 
  #declare mtime = str(5*clock*10/1000, -5, 3)  
  #declare gtime = str(floor(5*clock*10/1000/4), 2, 0)                          
  #declare textmessage1 = concat("Movement time : ", mtime, " s") 
  #declare textmessage2 = concat("Growth time : ", gtime, " hours")
                              
  text {           
    ttf "timrom.ttf" textmessage2 0.05, 0.1*x
    pigment {color rgb <1.000, 0.000, 0.000>  }     
        scale <30,30,30> 
        translate <-900,1000,-75>  
        rotate 0  
	
  }    
  text {           
    ttf "timrom.ttf" textmessage1 0.05, 0.1*x
    pigment {color rgb <1.000, 0.000, 0.000>  }     
        scale <30,30,30> 
        translate <-900,1040,-75>  
        rotate 0  
	
  }
    
                    
  union{     
  
    box {                                                                                                                                                                   
      <-Lx, -2, -Lz>,  // Near lower left corner
      <2*Lx, 0, 2*Lz>
      texture {
        pigment{
          color rgb <0.25, 0.25, 0.25>
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
