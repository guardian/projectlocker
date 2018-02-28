import React from 'react';
import GeneralDeleteComponent from './GeneralDeleteComponent.jsx';
import SummaryComponent from '../multistep/projectcreate/SummaryComponent.jsx';
import ProjectEntryView from '../EntryViews/ProjectEntryView.jsx'

class ProjectEntryDeleteComponent extends GeneralDeleteComponent {
    constructor(props){
        super(props);
        this.itemClass = "Project";
        this.endpoint = "/api/project";
    }

    componentWillMount() {
        this.setState({loading: false});
        /*remove the default implementation, as the http request is made by ProjectEntryView*/
    }

    getSummary(){
        return <ProjectEntryView entryId={this.props.match.params.itemid}/>

    }
}

export default ProjectEntryDeleteComponent;