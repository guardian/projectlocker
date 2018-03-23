import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';
import FileEntryFilterComponent from './filter/FileEntryFilterComponent.jsx';
import {Link} from 'react-router-dom';

class FileEntryList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/file';
        this.filterEndpoint = '/api/file/list';

        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("File path","filepath"),
            GeneralListComponent.standardColumn("Storage", "storage"),
            GeneralListComponent.standardColumn("Owner", "user"),
            GeneralListComponent.standardColumn("Version","version"),
            GeneralListComponent.dateTimeColumn("Create time","ctime"),
            GeneralListComponent.dateTimeColumn("Modification time","mtime"),
            GeneralListComponent.dateTimeColumn("Access time","atime"),
            {
                header: "",
                key: "id",
                render: id=><Link to={"/file/" + id + "/delete"}><img className="smallicon" src="/assets/images/delete.png"/></Link>
            }
        ];
    }

    dependenciesDidLoad(){
        this.setState({filterTerms: this.props.location.search.includes("mine") ? {user: this.state.uid, match: "W_EXACT"} : {match: "W_CONTAINS"}})
    }

    getFilterComponent(){
        return <FileEntryFilterComponent filterDidUpdate={this.filterDidUpdate}
                                         filterTerms={this.state.filterTerms}
                                         isAdmin={this.state.isAdmin}/>
    }
}

export default FileEntryList;