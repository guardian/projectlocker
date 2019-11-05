import React from 'react';
import PropTypes from 'prop-types';
import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';
import PlutoProjectTypeSelector from '../../Selectors/PlutoProjectTypeSelector.jsx';

class TypeSelectorComponent extends CommonMultistepComponent {
    static propTypes = {
        projectTypes: PropTypes.array.isRequired,
        plutoTypesList: PropTypes.array.isRequired,
        selectedPlutoSubtype: PropTypes.number.isRequired,
        selectedType: PropTypes.number.isRequired,
        templateName: PropTypes.string.isRequired,
        valueWasSet: PropTypes.func.isRequired,
        deprecated: PropTypes.bool.isRequired,
        loadingComplete: PropTypes.boolean
    };

    constructor(props){
        super(props);

        this.state = {
            selectedType: props.selectedType,
            name: props.templateName,
            selectedPlutoSubtype: props.selectedPlutoSubtype,
            deprecated: props.deprecated
        };

        this.selectorValueChanged = this.selectorValueChanged.bind(this);
    }

    componentDidUpdate(prevProps,prevState){
        if(prevProps.loadingComplete===false && this.props.loadingComplete===true){
            this.setState({
                selectedType: this.props.selectedType,
                name: this.props.templateName,
                selectedPlutoSubtype: this.props.selectedPlutoSubtype,
                deprecated: this.props.deprecated
            });
        } else {
            super.componentDidUpdate(prevProps,prevState);
        }
    }


    selectorValueChanged(event){
        this.setState({name: this.props.templateName, selectedType: parseInt(event.target.value), deprecated: this.props.deprecated}, ()=>
            this.updateParent()
        );
    }

    projectTypeForId(projectTypeId){
        for(let n=0;n<this.props.projectTypes.length;++n){
            if(this.props.projectTypes[n].id===projectTypeId) return this.props.projectTypes[n];
        }
        return null;
    }

    getPlutoSubtypeForPlType(){
        const type = this.projectTypeForId(this.props.selectedType);
        console.log(type);
        if(!type) return "";

        return type.hasOwnProperty('plutoType') ? type.plutoType : null
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
                <label htmlFor="pluto_subtype_selector">Pluto subtype, if applicable:</label>
                <PlutoProjectTypeSelector id="pluto_subtype_selector"
                                          plutoProjectTypesList={this.props.plutoTypesList}
                                          selectionUpdated={newValue=>this.setState({selectedPlutoSubtype: newValue})}
                                          selectedType={this.state.selectedPlutoSubtype}
                                          onlyShowSubtypes={true}
                                          subTypesFor={this.getPlutoSubtypeForPlType()}/>
                <label htmlFor="projectNameSelector">Template name:</label>
                <input type="text" id="projectNameSelector" value={this.state.name}
                           onChange={(event)=>this.setState({name: event.target.value})}/>
                <label htmlFor="projectDeprecatedSelector">Deprecated:</label>
                <input type="checkbox" id="projectDeprecatedSelector" checked={this.state.deprecated} onChange={(event)=>this.setState({deprecated: event.target.checked})}/>
            </div>
        )

    }

    updateParent(){
        this.props.valueWasSet(this.state);
    }
}

export default TypeSelectorComponent;
