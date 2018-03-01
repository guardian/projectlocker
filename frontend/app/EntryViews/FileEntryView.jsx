import React from 'react';
import GenericEntryView from './GenericEntryView.jsx';
import StorageEntryView from './StorageEntryView.jsx';

class FileEntryView extends GenericEntryView {
    constructor(props){
        super(props);
        this.endpoint = "/api/file"
    }

    render(){
        return <span>{this.state.content.filepath} on <StorageEntryView entryId={this.state.content.storage}/></span>
    }
}

export default FileEntryView;
