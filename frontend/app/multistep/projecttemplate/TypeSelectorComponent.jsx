import React from 'react';
import axios from 'axios';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class TypeSelectorComponent extends CommonMultistepComponent {
    constructor(props){
        super(props);

        this.state = {
            selectedType: props.selectedType,
            name: ""
        };

        this.selectorValueChanged = this.selectorValueChanged.bind(this);
    }


    updateParent(){
        this.props.valueWasSet(this.state);
    }

    selectorValueChanged(event){
        this.setState({selectedType: parseInt(event.target.value)}, ()=>
            this.updateParent()
        );
    }

    render() {
        return(<div>
                <h3>Project Type and Name</h3>
                <p className="information">The first pieces of information we need are what kind of project this template represents and what it should be called.
                    Please select from the list below.  If the right type of project is not present, please <a href="/type/new">add</a> it and then come back to this form.
                </p>
                <label htmlFor="project_type_selector">Project type:</label>
                <select id="project_type_selector" value={this.props.selectedType} onChange={this.selectorValueChanged}>
                    {
                    this.props.projectTypes.map((projectInfo, index)=><option key={index} value={projectInfo.id}>{projectInfo.name}</option>)
                    }
                </select>
            <label htmlFor="projectNameSelector">Project name:</label>
                    <input type="text" id="projectNmaeSelector" onChange={(event)=>this.setState({name: event.target.value}, ()=>this.updateParent())}/>
            </div>
        )

    }
}

export default TypeSelectorComponent;
