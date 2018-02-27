import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class TypeSelectorComponent extends CommonMultistepComponent {
    static propTypes = {
        projectTypes: PropTypes.array.isRequired,
        selectedType: PropTypes.number.isRequired,
        templateName: PropTypes.string.isRequired
    };

    constructor(props){
        super(props);

        this.state = {
            selectedType: props.selectedType,
            name: ""
        };

        this.selectorValueChanged = this.selectorValueChanged.bind(this);
    }

    componentWillMount(){
        this.setState({

        })
    }

    updateParent(){
        this.props.valueWasSet(this.state);
    }

    selectorValueChanged(event){
        this.setState({name: this.props.templateName, selectedType: parseInt(event.target.value)}, ()=>
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
                    <input type="text" id="projectNameSelector" value={this.props.templateName}
                           onChange={(event)=>this.setState({name: event.target.value, selectedType: this.props.selectedType}, ()=>this.updateParent())}/>
            </div>
        )

    }
}

export default TypeSelectorComponent;
