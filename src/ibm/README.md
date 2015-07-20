This is where the calculations take place. It is recommended to read the first paper published on this model ("Variable Cell Morphology Approach for Individual-Based Modeling of Microbial Communities") to get an understanding of the model inner workings.

Model hierarchy
---------------
The key classes in the model are as follows:

```
├── Model
│   ├── Cell
│   │   ├── Ball
│   │   ├── Spring
│   │   │   ├── AnchorSpring
│   │   │   ├── FilSpring
│   │   │   ├── StickSpring
```

Other classes take care of interfacing with the user (```Interface```), define the order in which operations are executed (```Run*```) or provide low-level support for the key classes (```Common```, ```RelaxationODE``` and ```Vector3d```).

FAQ
---

**Why are all fields in Model public?**

The ser2mat class (different package) needs to read the fields from Model. This could be fixed by creating a generic getter method in Model that uses a string as input (the code can probably be pieced together from the ser2mat code). This method would need to return a dynamic type


**Can I create a new Ball/Cell without adding it to the model.ballArray/model.cellArray?**

No. I did this so I wouldn't forget to add balls/cells to the model. If you want to do this, simply add an if wrapper around the add method in Ball and Cell


**Why is there a reference to the model everywhere?**

Most parameters are stored in the model. Why not use a static Model class then, so we can access parameters anywhere? I believe it is considered good practise that static classes cannot be serialized in Java, therefore they have disallowed it. It might be possible to externalize the model instead of serializing it, haven't looked into this.
