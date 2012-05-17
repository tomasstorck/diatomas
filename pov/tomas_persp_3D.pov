// Edited by Tomas, to work with his model, from Dec 2011. Based on BS7new_persp_3D.pov by Damien or Matt
  #declare Animation  = 1;
  #declare viewmode   = 1;
                      
  #declare FileNumber = str(clock,-10, 4)	// first 5 is movement, last 5 is growth iter, dot in the middle
  #declare PathInc    = "./output/"		//Since pov is called from the model's rootDir we won't need to supply it
  #declare FullName   = concat(PathInc,"model.",FileNumber,".pov")

//  #declare Lx = 1000;	// more zoomed in
//  #declare Lz = 1000;      
//  #declare Ly = 1000; 
  
  #declare Lx = 1800;
  #declare Lz = 1800;      
  #declare Ly = 1800;
  

  camera {                   
    #if (viewmode=1)
      // view in perspective
//      location <-1*Lx, 0.9*Ly,-0.7*Lz> 
//      look_at  <Lx/2, Ly/4, Lz/2>
    	location <-1*Lx, 0.9*Ly,-0.7*Lz> 
	    look_at  <Lx/2, 0, Lz/2>
      
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
 
//Matlab is better with those variables, though slower
//  #declare mtime = str(5*clock*10/1000, -5, 3)  
//  #declare gtime = str(floor(5*clock*10/1000/4), 2, 0)                          
//  #declare textmessage1 = concat("Movement time : ", mtime, " s") 
//  #declare textmessage2 = concat("Growth time : ", gtime, " hours")
                              
/*  text {           
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
	
  } */
    
                    
  union{     
  
    box {
      <-Lx, 0, -Lz>,  	// Position: in the middle
      <2*Lx, 0, 2*Lz>	// Size: 2*L
      texture {
        pigment{
          color rgb <0.2, 0.2, 0.2>
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