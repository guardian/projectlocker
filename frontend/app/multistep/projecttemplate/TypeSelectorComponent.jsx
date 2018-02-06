import React from 'react';
import axios from 'axios';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class TypeSelectorComponent extends CommonMultistepComponent {
    constructor(props){
        super(props);

        this.selectorValueChanged = this.selectorValueChanged.bind(this);
    }

    selectorValueChanged(event){
        this.props.valueWasSet(parseInt(event.target.value));
    }

    render() {
        return(<div>
                <h3>Project Type</h3>
                <p className="information">The first piece of information we need is what kind of project this template represents.
                    Please select from the list below.  If the right type of project is not present, please <a href="/type/new">add</a>
                     it and then come back to this form.
                </p>
                <select id="project_type_selector" value={this.props.selectedType} onChange={this.selectorValueChanged}>
                    {
                    this.props.projectTypes.map((projectInfo, index)=><option key={index} value={projectInfo.id}>{projectInfo.name}</option>)
                    }
                </select>
            </div>
        )

    }
}

export default TypeSelectorComponent;
