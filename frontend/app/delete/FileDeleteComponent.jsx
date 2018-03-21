import React from 'react';
import GeneralDeleteComponent from './GeneralDeleteComponent.jsx';
import SummaryComponent from '../multistep/projectcreate/SummaryComponent.jsx';
import FileEntryView from '../EntryViews/FileEntryView.jsx'
import FileReferencesView from '../EntryViews/FileReferencesView.jsx';

class FileEntryDeleteComponent extends GeneralDeleteComponent {
    constructor(props){
        super(props);
        this.itemClass = "File";
        this.endpoint = "/api/file";
    }

    componentWillMount() {
        this.setState({loading: false});
        /*remove the default implementation, as the http request is made by FileEntryView*/
    }
    
    getSummary(){
        return <div>
            <FileEntryView entryId={this.props.match.params.itemid}/>
            <FileReferencesView entryId={this.props.match.params.itemid}/>
        </div>
    }
}

export default FileEntryDeleteComponent;