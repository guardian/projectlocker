import React from 'react';
import SortableTable from 'react-sortable-table';
import GeneralListComponent from './GeneralListComponent.jsx';

class ProjectTemplateIndex extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/template';
    }

    render() {
        const columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'}
            },
            {
                header: "",
                key: "name"
            },
            {
                header: "Project type",
                key: "projectType"
            },
            {
                header: "Filepath",
                key: "filepath"
            },
            {
                header: "Storage",
                key: "storage"
            }
        ];

        const style = {
            backgroundColor: '#eee'
        };

        const iconStyle = {
            color: '#aaa',
            paddingLeft: '5px',
            paddingRight: '5px'
        };

        return (
            <SortableTable
                data={ this.state.data}
                columns={columns}
                style={style}
                iconStyle={iconStyle}
            />
        );

    }
}

export default ProjectTemplateIndex;